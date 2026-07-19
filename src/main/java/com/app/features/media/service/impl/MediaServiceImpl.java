package com.app.features.media.service.impl;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.app.config.settings.AppProperties.AllowedMediaType;
import com.app.core.enums.RecordStatus;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.job.MediaProcessingJob;
import com.app.features.media.repository.MediaRepository;
import com.app.features.media.repository.spec.MediaSpecification;
import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.media.schema.payload.CreateMediaPayload;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.service.MediaService;
import com.app.features.media.storage.MediaFileStorage;
import com.app.features.media.storage.StagedMediaFile;
import com.app.features.media.storage.StoredMediaFile;
import com.app.features.media.validation.MediaFileValidator;
import com.app.features.media.validation.MediaTypePolicyResolver;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.repository.UserBaseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final UserBaseRepository userBaseRepository;
    private final MediaFileStorage mediaFileStorage;
    private final MediaTypePolicyResolver mediaTypePolicyResolver;
    private final MediaFileValidator mediaFileValidator;
    private final JobScheduler jobScheduler;
    private final ModelMapper mapper;

    @Transactional
    @Override
    public MediaResult createMedia(UUID createdById, CreateMediaPayload payload) {
        if (payload == null || payload.getFile() == null) {
            throw ExceptionFactory.invalidParam("Media file is required.");
        }

        UserBaseEntity creator = userBaseRepository.findById(createdById)
                .orElseThrow(() -> ExceptionFactory.notFound("User: " + createdById));

        MultipartFile upload = payload.getFile();
        AllowedMediaType policy = mediaTypePolicyResolver.resolve(upload.getOriginalFilename());
        StagedMediaFile stagedFile = mediaFileStorage.stage(upload, policy);
        StoredMediaFile storedFile;
        try {
            String detectedContentType = mediaFileValidator.validate(
                    stagedFile.getTemporaryPath(),
                    policy);
            storedFile = mediaFileStorage.commit(stagedFile, detectedContentType);
        } catch (RuntimeException ex) {
            mediaFileStorage.discard(stagedFile);
            throw ex;
        }

        registerRollbackCleanup(storedFile.getStorageKey());

        MediaEntity media = new MediaEntity();
        media.setCreatedBy(creator);
        media.setStorageKey(storedFile.getStorageKey());
        media.setPublicKey(UUID.randomUUID().toString());
        media.setOriginalName(storedFile.getOriginalName());
        media.setContentType(storedFile.getContentType());
        media.setFileSize(storedFile.getFileSize());
        media.setKind(policy.getKind());
        media.setProcessingStatus(requiresProcessing(policy.getKind())
                ? MediaProcessingStatus.PENDING
                : MediaProcessingStatus.READY);
        media.setStatus(RecordStatus.ACTIVE);

        media = mediaRepository.save(media);
        if (media.getProcessingStatus() == MediaProcessingStatus.PENDING) {
            registerProcessingJob(media.getId());
        }

        return mapper.map(media, MediaResult.class);
    }

    @Transactional
    @Override
    public void deleteOwnedMedia(UUID mediaId, UUID createdById) {
        MediaEntity media = mediaRepository.findByIdAndCreatedBy_Id(mediaId, createdById)
                .orElseThrow(() -> ExceptionFactory.notFound("Media: " + mediaId));

        deleteMediaEntity(media);
    }

    @Transactional
    @Override
    public void deleteMedia(UUID mediaId) {
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> ExceptionFactory.notFound("Media: " + mediaId));

        deleteMediaEntity(media);
    }

    @Override
    public Page<MediaResult> getManyMedia(MediaFilterCriteria criteria, Pageable pageable) {
        Specification<MediaEntity> specification = MediaSpecification.withFilter(criteria);
        Page<MediaEntity> entityPage = mediaRepository.findAll(specification, pageable);

        return entityPage.map(entity -> mapper.map(entity, MediaResult.class));
    }

    @Override
    public List<MediaEntity> requireOwnedActiveMedia(
            Collection<UUID> mediaIds,
            UUID createdById) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<UUID> distinctIds = new LinkedHashSet<>(mediaIds);

        if (distinctIds.size() != mediaIds.size()) {
            throw ExceptionFactory.invalidParam("Duplicate media IDs are not allowed.");
        }

        List<MediaEntity> media = mediaRepository.findAllByIdInAndCreatedBy_IdAndStatusAndProcessingStatus(
                distinctIds,
                createdById,
                RecordStatus.ACTIVE,
                MediaProcessingStatus.READY);

        if (media.size() != distinctIds.size()) {
            throw ExceptionFactory.invalidParam(
                    "Some media are missing, inactive, or owned by another user.");
        }

        return media;
    }

    private boolean requiresProcessing(MediaKind kind) {
        return kind == MediaKind.VIDEO || kind == MediaKind.AUDIO;
    }

    private void registerProcessingJob(UUID mediaId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    jobScheduler.<MediaProcessingJob>enqueue(job -> job.execute(mediaId));
                } catch (RuntimeException ex) {
                    log.error("Failed to enqueue media processing job [{}]", mediaId, ex);
                }
            }
        });
    }

    private void deleteMediaEntity(MediaEntity media) {
        mediaRepository.delete(media);
        registerAfterCommitCleanup(media.getStorageKey());
    }

    private void registerRollbackCleanup(String storageKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteFileSafely(storageKey);
                }
            }
        });
    }

    private void registerAfterCommitCleanup(String storageKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFileSafely(storageKey);
            }
        });
    }

    private void deleteFileSafely(String storageKey) {
        try {
            mediaFileStorage.delete(storageKey);
        } catch (RuntimeException ex) {
            log.error("Failed to delete media file [{}]", storageKey, ex);
        }
    }
}
