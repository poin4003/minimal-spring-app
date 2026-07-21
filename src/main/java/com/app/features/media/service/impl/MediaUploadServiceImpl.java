package com.app.features.media.service.impl;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.settings.AppProperties;
import com.app.config.settings.AppProperties.AllowedMediaType;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.entity.MediaUploadSessionEntity;
import com.app.features.media.enums.MediaUploadStatus;
import com.app.features.media.repository.MediaRepository;
import com.app.features.media.repository.MediaUploadSessionRepository;
import com.app.features.media.schema.model.MediaUploadAssemblyContext;
import com.app.features.media.schema.payload.StartMediaUploadPayload;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.schema.result.MediaUploadSessionResult;
import com.app.features.media.service.MediaService;
import com.app.features.media.service.MediaUploadService;
import com.app.features.media.storage.MediaChunkStorage;
import com.app.features.media.storage.MediaFileStorage;
import com.app.features.media.storage.MediaFilenameSupport;
import com.app.features.media.storage.schema.StagedMediaFile;
import com.app.features.media.validation.MediaTypePolicyResolver;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.repository.UserBaseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaUploadServiceImpl implements MediaUploadService {

    private final MediaUploadSessionRepository mediaUploadSessionRepo;
    private final MediaRepository mediaRepo;
    private final UserBaseRepository userBaseRepo;
    private final MediaService mediaSvc;
    private final MediaChunkStorage mediaChunkStorage;
    private final MediaFileStorage mediaFileStorage;
    private final MediaTypePolicyResolver mediaTypePolicyResolver;
    private final ModelMapper mapper;
    private final AppProperties appProperties;

    @Transactional
    @Override
    public MediaUploadSessionResult startUpload(
            UUID createdById,
            StartMediaUploadPayload payload) {
        String originalName = MediaFilenameSupport.normalize(payload.getOriginalName());
        AllowedMediaType policy = mediaTypePolicyResolver.resolve(originalName);
        if (payload.getFileSize() > policy.getMaxFileSizeBytes()) {
            throw ExceptionFactory.invalidParam("Media file exceeds the allowed size.");
        }

        UserBaseEntity creator = userBaseRepo.findById(createdById)
                .orElseThrow(() -> ExceptionFactory.notFound("User: " + createdById));
        AppProperties.ChunkUpload config = appProperties.getMedia().getChunkUpload();
        long totalChunks = Math.ceilDiv(payload.getFileSize(), (long) config.getChunkSizeBytes());
        if (totalChunks > Integer.MAX_VALUE) {
            throw ExceptionFactory.invalidParam("Media upload contains too many chunks.");
        }

        MediaUploadSessionEntity session = new MediaUploadSessionEntity();
        session.setId(UUID.randomUUID());
        session.setCreatedBy(creator);
        session.setOriginalName(originalName);
        session.setFileSize(payload.getFileSize());
        session.setChunkSize(config.getChunkSizeBytes());
        session.setTotalChunks(Math.toIntExact(totalChunks));
        session.setStatus(MediaUploadStatus.UPLOADING);
        session.setExpiresAt(LocalDateTime.now().plus(config.getSessionTtl()));

        session = mediaUploadSessionRepo.save(session);
        return toResult(session, List.of());
    }

    @Transactional(readOnly = true)
    @Override
    public MediaUploadSessionResult getUpload(UUID uploadId, UUID createdById) {
        MediaUploadSessionEntity session = requireOwnedSession(uploadId, createdById);
        validateNotExpired(session);
        List<Integer> uploadedChunks = session.getStatus() == MediaUploadStatus.COMPLETED
                ? List.of()
                : mediaChunkStorage.findUploadedChunks(uploadId);
        return toResult(session, uploadedChunks);
    }

    @Override
    public void uploadChunk(
            UUID uploadId,
            UUID createdById,
            int chunkIndex,
            long contentLength,
            String checksum,
            InputStream inputStream) {
        MediaUploadSessionEntity session = requireOwnedSession(uploadId, createdById);
        validateUploading(session);
        long expectedSize = expectedChunkSize(session, chunkIndex);
        if (contentLength >= 0 && contentLength != expectedSize) {
            throw ExceptionFactory.invalidParam("Media chunk content length is invalid.");
        }

        mediaChunkStorage.storeChunk(
                uploadId,
                chunkIndex,
                expectedSize,
                checksum,
                inputStream);
        touchUpload(uploadId, createdById);
    }

    @Override
    public MediaResult completeUpload(UUID uploadId, UUID createdById) {
        MediaUploadSessionEntity currentSession = requireOwnedSession(uploadId, createdById);
        if (currentSession.getStatus() == MediaUploadStatus.COMPLETED) {
            return completedMediaResult(currentSession);
        }

        MediaUploadAssemblyContext context = beginCompletion(uploadId, createdById);
        if (context == null) {
            return completedMediaResult(requireOwnedSession(uploadId, createdById));
        }
        StagedMediaFile stagedFile = null;
        try {
            stagedFile = mediaChunkStorage.assemble(
                    context.getUploadId(),
                    context.getOriginalName(),
                    context.getExtension(),
                    context.getFileSize(),
                    context.getTotalChunks());
            MediaResult result = finalizeCompletion(uploadId, createdById, stagedFile);
            deleteUploadChunksSafely(uploadId);
            return result;
        } catch (RuntimeException ex) {
            if (stagedFile != null) {
                mediaFileStorage.discard(stagedFile);
            }
            resetCompletionSafely(uploadId, createdById);
            throw ex;
        }
    }

    @Override
    public void cancelUpload(UUID uploadId, UUID createdById) {
        deleteSession(uploadId, createdById);
        deleteUploadChunksSafely(uploadId);
    }

    @Transactional
    private void touchUpload(UUID uploadId, UUID createdById) {
        MediaUploadSessionEntity session = requireLockedSession(uploadId, createdById);
        validateUploading(session);
        session.setExpiresAt(LocalDateTime.now().plus(
                appProperties.getMedia().getChunkUpload().getSessionTtl()));
    }

    @Transactional
    private MediaUploadAssemblyContext beginCompletion(UUID uploadId, UUID createdById) {
        MediaUploadSessionEntity session = requireLockedSession(uploadId, createdById);
        validateNotExpired(session);
        if (session.getStatus() == MediaUploadStatus.COMPLETED) {
            return null;
        }
        if (session.getStatus() != MediaUploadStatus.UPLOADING) {
            throw ExceptionFactory.invalidParam("Media upload cannot be assembled in its current state.");
        }

        session.setStatus(MediaUploadStatus.ASSEMBLING);
        session.setExpiresAt(LocalDateTime.now().plus(
                appProperties.getMedia().getChunkUpload().getSessionTtl()));
        return new MediaUploadAssemblyContext(
                session.getId(),
                session.getOriginalName(),
                MediaFilenameSupport.extensionOf(session.getOriginalName()),
                session.getFileSize(),
                session.getTotalChunks());
    }

    @Transactional
    private MediaResult finalizeCompletion(
            UUID uploadId,
            UUID createdById,
            StagedMediaFile stagedFile) {
        MediaUploadSessionEntity session = requireLockedSession(uploadId, createdById);
        if (session.getStatus() != MediaUploadStatus.ASSEMBLING) {
            throw ExceptionFactory.invalidParam("Media upload is not being assembled.");
        }

        MediaResult result = mediaSvc.createMedia(createdById, stagedFile);
        MediaEntity completedMedia = mediaRepo.getReferenceById(result.getId());
        session.setCompletedMedia(completedMedia);
        session.setStatus(MediaUploadStatus.COMPLETED);
        session.setExpiresAt(LocalDateTime.now().plus(
                appProperties.getMedia()
                        .getChunkUpload()
                        .getCompletedSessionRetention()));
        return result;
    }

    @Transactional
    private void resetCompletion(UUID uploadId, UUID createdById) {
        MediaUploadSessionEntity session = requireLockedSession(uploadId, createdById);
        if (session.getStatus() == MediaUploadStatus.ASSEMBLING) {
            session.setStatus(MediaUploadStatus.UPLOADING);
            session.setExpiresAt(LocalDateTime.now().plus(
                    appProperties.getMedia().getChunkUpload().getSessionTtl()));
        }
    }

    @Transactional
    private void deleteSession(UUID uploadId, UUID createdById) {
        MediaUploadSessionEntity session = requireLockedSession(uploadId, createdById);
        if (session.getStatus() == MediaUploadStatus.COMPLETED) {
            throw ExceptionFactory.invalidParam("Completed media upload cannot be cancelled.");
        }
        mediaUploadSessionRepo.delete(session);
    }

    private MediaUploadSessionEntity requireOwnedSession(UUID uploadId, UUID createdById) {
        return mediaUploadSessionRepo.findByIdAndCreatedBy_Id(uploadId, createdById)
                .orElseThrow(() -> ExceptionFactory.notFound("Media upload: " + uploadId));
    }

    private MediaUploadSessionEntity requireLockedSession(UUID uploadId, UUID createdById) {
        return mediaUploadSessionRepo.findOneByIdAndCreatedBy_Id(uploadId, createdById)
                .orElseThrow(() -> ExceptionFactory.notFound("Media upload: " + uploadId));
    }

    private void validateUploading(MediaUploadSessionEntity session) {
        validateNotExpired(session);
        if (session.getStatus() != MediaUploadStatus.UPLOADING) {
            throw ExceptionFactory.invalidParam("Media upload is not accepting chunks.");
        }
    }

    private void validateNotExpired(MediaUploadSessionEntity session) {
        if (session.getStatus() != MediaUploadStatus.COMPLETED
                && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ExceptionFactory.invalidParam("Media upload session has expired.");
        }
    }

    private long expectedChunkSize(MediaUploadSessionEntity session, int chunkIndex) {
        if (chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw ExceptionFactory.invalidParam("Media chunk index is invalid.");
        }

        long offset = (long) chunkIndex * session.getChunkSize();
        return Math.min(session.getChunkSize(), session.getFileSize() - offset);
    }

    private MediaUploadSessionResult toResult(
            MediaUploadSessionEntity session,
            List<Integer> uploadedChunks) {
        MediaUploadSessionResult result = mapper.map(
                session,
                MediaUploadSessionResult.class);
        result.setUploadedChunks(uploadedChunks);
        if (session.getCompletedMedia() != null) {
            result.setCompletedMedia(mapper.map(session.getCompletedMedia(), MediaResult.class));
        }
        return result;
    }

    private MediaResult completedMediaResult(MediaUploadSessionEntity session) {
        if (session.getCompletedMedia() == null) {
            throw ExceptionFactory.serverError("Completed media upload has no media record.");
        }
        return mapper.map(session.getCompletedMedia(), MediaResult.class);
    }

    private void deleteUploadChunksSafely(UUID uploadId) {
        try {
            mediaChunkStorage.deleteUpload(uploadId);
        } catch (RuntimeException ex) {
            log.warn("Failed to delete completed media upload chunks [{}].", uploadId, ex);
        }
    }

    private void resetCompletionSafely(UUID uploadId, UUID createdById) {
        try {
            resetCompletion(uploadId, createdById);
        } catch (RuntimeException ex) {
            log.error("Failed to reset media upload session [{}].", uploadId, ex);
        }
    }
}
