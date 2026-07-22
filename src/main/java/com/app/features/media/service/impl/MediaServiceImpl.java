package com.app.features.media.service.impl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.config.settings.AppProperties.AllowedMediaType;
import com.app.core.enums.RecordStatus;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.entity.MediaEntity_;
import com.app.features.media.entity.MediaProcessingLeaseEntity;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.job.MediaProcessingJob;
import com.app.features.media.repository.MediaRepository;
import com.app.features.media.repository.MediaProcessingLeaseRepository;
import com.app.features.media.repository.MediaVariantRepository;
import com.app.features.media.repository.spec.MediaSpecification;
import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.media.schema.model.MediaThumbnailResult;
import com.app.features.media.schema.payload.CreateMediaPayload;
import com.app.features.media.schema.result.MediaDetailResult;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.schema.result.MediaVariantResult;
import com.app.features.media.service.MediaService;
import com.app.features.media.service.MediaThumbnailService;
import com.app.features.media.storage.MediaFileStorage;
import com.app.features.media.storage.schema.StagedMediaFile;
import com.app.features.media.storage.schema.StoredMediaFile;
import com.app.features.media.support.MediaProcessingPolicy;
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

    private final MediaRepository mediaRepo;
    private final UserBaseRepository userBaseRepo;
    private final MediaFileStorage mediaFileStorage;
    private final MediaVariantRepository mediaVariantRepo;
    private final MediaProcessingLeaseRepository mediaProcessingLeaseRepo;
    private final MediaTypePolicyResolver mediaTypePolicyResolver;
    private final MediaFileValidator mediaFileValidator;
    private final MediaThumbnailService mediaThumbnailSvc;
    private final MediaProcessingPolicy mediaProcessingPolicy;
    private final JobScheduler jobScheduler;
    private final ModelMapper mapper;
    private final AppProperties appProperties;

    @Transactional
    @Override
    public MediaResult createMedia(UUID createdById, CreateMediaPayload payload) {
        if (payload == null || payload.getFile() == null) {
            throw ExceptionFactory.invalidParam("Media file is required.");
        }

        MultipartFile upload = payload.getFile();
        AllowedMediaType policy = mediaTypePolicyResolver.resolve(upload.getOriginalFilename());
        StagedMediaFile stagedFile = mediaFileStorage.stage(upload, policy);
        return createStagedMedia(createdById, stagedFile, policy);
    }

    @Transactional
    @Override
    public MediaResult createMedia(UUID createdById, StagedMediaFile stagedFile) {
        if (stagedFile == null) {
            throw ExceptionFactory.invalidParam("Staged media file is required.");
        }

        AllowedMediaType policy = mediaTypePolicyResolver.resolve(stagedFile.getOriginalName());
        return createStagedMedia(createdById, stagedFile, policy);
    }

    private MediaResult createStagedMedia(
            UUID createdById,
            StagedMediaFile stagedFile,
            AllowedMediaType policy) {
        StoredMediaFile storedFile;
        UserBaseEntity creator;
        try {
            creator = userBaseRepo.findById(createdById)
                    .orElseThrow(() -> ExceptionFactory.notFound("User: " + createdById));
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
        media.setProcessingStatus(mediaProcessingPolicy.requiresProcessing(
                policy.getKind(),
                storedFile.getContentType())
                ? MediaProcessingStatus.PENDING
                : MediaProcessingStatus.READY);
        media.setStatus(RecordStatus.ACTIVE);

        media = mediaRepo.save(media);
        MediaProcessingLeaseEntity processingLease = new MediaProcessingLeaseEntity();
        processingLease.setMediaId(media.getId());
        mediaProcessingLeaseRepo.save(processingLease);

        if (media.getProcessingStatus() == MediaProcessingStatus.PENDING) {
            registerProcessingJob(media.getId());
        }

        return toMediaResult(media);
    }

    @Transactional
    @Override
    public void deleteOwnedMedia(UUID mediaId, UUID createdById) {
        MediaEntity media = mediaRepo.findByIdAndCreatedBy_Id(mediaId, createdById)
                .orElseThrow(() -> ExceptionFactory.notFound("Media: " + mediaId));

        deleteMediaEntity(media);
    }

    @Transactional
    @Override
    public void deleteMedia(UUID mediaId) {
        MediaEntity media = mediaRepo.findById(mediaId)
                .orElseThrow(() -> ExceptionFactory.notFound("Media: " + mediaId));

        deleteMediaEntity(media);
    }

    @Override
    public Page<MediaResult> getManyMedia(MediaFilterCriteria criteria, Pageable pageable) {
        Specification<MediaEntity> specification = MediaSpecification.withFilter(criteria);
        Page<MediaEntity> entityPage = mediaRepo.findAll(specification, pageable);

        return entityPage.map(entity -> toMediaResult(entity));
    }

    @Override
    public Page<MediaResult> getManyOwnedMedia(
            UUID ownerId,
            MediaFilterCriteria criteria,
            Pageable pageable) {
        MediaFilterCriteria ownedCriteria = mapper.map(criteria, MediaFilterCriteria.class);
        ownedCriteria.setCreatedById(ownerId);

        return getManyMedia(ownedCriteria, pageable);
    }

    @Override
    public MediaDetailResult getMediaDetail(UUID mediaId) {
        MediaEntity media = mediaRepo.findOneById(mediaId)
                .orElseThrow(() -> ExceptionFactory.notFound("Media: " + mediaId));

        return toMediaDetailResult(media);
    }

    @Override
    public MediaDetailResult getOwnedMediaDetail(UUID mediaId, UUID ownerId) {
        MediaEntity media = mediaRepo.findByIdAndCreatedBy_Id(mediaId, ownerId)
                .orElseThrow(() -> ExceptionFactory.notFound("Media: " + mediaId));

        return toMediaDetailResult(media);
    }

    @Transactional
    @Override
    public MediaResult retryProcessing(UUID mediaId) {
        MediaEntity media = mediaRepo.findById(mediaId)
                .orElseThrow(() -> ExceptionFactory.notFound("Media: " + mediaId));

        return retryMedia(media);
    }

    @Transactional
    @Override
    public MediaResult retryOwnedProcessing(UUID mediaId, UUID ownerId) {
        MediaEntity media = mediaRepo.findByIdAndCreatedBy_Id(mediaId, ownerId)
                .orElseThrow(() -> ExceptionFactory.notFound("Media: " + mediaId));

        return retryMedia(media);
    }

    @Transactional
    @Override
    public MediaResult updateOwnedThumbnail(
            UUID mediaId,
            UUID ownerId,
            UUID thumbnailMediaId) {
        MediaEntity targetMedia = mediaRepo.findByIdAndCreatedBy_Id(mediaId, ownerId)
                .orElseThrow(() -> ExceptionFactory.notFound("Media: " + mediaId));
        MediaEntity thumbnailMedia = mediaRepo
                .findByIdAndCreatedBy_Id(thumbnailMediaId, ownerId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "Media: " + thumbnailMediaId));

        requireReadyActiveMedia(targetMedia, "Target media");
        requireReadyActiveMedia(thumbnailMedia, "Thumbnail media");
        if (!mediaProcessingPolicy.supportsManualThumbnail(targetMedia.getKind())) {
            throw ExceptionFactory.invalidParam(
                    "Only video and audio media support a custom thumbnail.");
        }
        if (thumbnailMedia.getKind() != MediaKind.IMAGE
                || thumbnailMedia.getThumbnailStorageKey() == null
                || thumbnailMedia.getThumbnailStorageKey().isBlank()) {
            throw ExceptionFactory.invalidParam(
                    "Source media must be a ready image with a generated thumbnail.");
        }

        MediaThumbnailResult thumbnail = mediaThumbnailSvc.copyThumbnail(
                targetMedia,
                thumbnailMedia);
        targetMedia.setThumbnailStorageKey(thumbnail.getStorageKey());
        return toMediaResult(targetMedia);
    }

    @Override
    public int recoverPendingMedia() {
        AppProperties.MediaMaintenance config = appProperties.getMedia().getMaintenance();
        LocalDateTime cutoff = LocalDateTime.now().minus(config.getPendingRecoveryTtl());
        Sort recoverySort = Sort.by(
                Sort.Direction.ASC,
                MediaEntity_.UPDATED_AT,
                MediaEntity_.ID);
        Pageable firstPageable = PageRequest.of(0, config.getBatchSize(), recoverySort);
        Page<MediaEntity> firstPage = mediaRepo
                .findAllByStatusAndProcessingStatusAndUpdatedAtBefore(
                        RecordStatus.ACTIVE,
                        MediaProcessingStatus.PENDING,
                        cutoff,
                        firstPageable);

        int enqueued = 0;
        for (int pageNumber = firstPage.getTotalPages() - 1;
                pageNumber >= 0;
                pageNumber--) {
            Page<MediaEntity> candidates = pageNumber == 0
                    ? firstPage
                    : mediaRepo.findAllByStatusAndProcessingStatusAndUpdatedAtBefore(
                            RecordStatus.ACTIVE,
                            MediaProcessingStatus.PENDING,
                            cutoff,
                            PageRequest.of(pageNumber, config.getBatchSize(), recoverySort));

            for (MediaEntity media : candidates.getContent()) {
                try {
                    enqueueProcessingJob(media.getId());
                    enqueued++;
                } catch (RuntimeException ex) {
                    log.error("Failed to recover pending media [{}]", media.getId(), ex);
                }
            }
        }

        return enqueued;
    }

    private MediaResult retryMedia(MediaEntity media) {
        if (media.getStatus() != RecordStatus.ACTIVE) {
            throw ExceptionFactory.invalidParam("Inactive media cannot be processed.");
        }

        if (!mediaProcessingPolicy.requiresProcessing(
                media.getKind(),
                media.getContentType())) {
            throw ExceptionFactory.invalidParam(
                    "This media type does not require processing.");
        }

        if (media.getProcessingStatus() != MediaProcessingStatus.FAILED) {
            throw ExceptionFactory.invalidParam(
                    "Only failed media can be retried.");
        }

        media.setProcessingStatus(MediaProcessingStatus.PENDING);
        media = mediaRepo.save(media);
        registerProcessingJob(media.getId());

        return toMediaResult(media);
    }

    private MediaDetailResult toMediaDetailResult(MediaEntity media) {
        MediaDetailResult result = mapper.map(media, MediaDetailResult.class);
        List<MediaVariantResult> variants = mediaVariantRepo
                .findAllByMedia_IdOrderByVariantTypeAscHeightAsc(media.getId())
                .stream()
                .map(entity -> mapper.map(entity, MediaVariantResult.class))
                .toList();

        result.setVariants(variants);
        result.setThumbnailUrl(resolveThumbnailUrl(media));
        return result;
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

        List<MediaEntity> media = mediaRepo.findAllByIdInAndCreatedBy_IdAndStatusAndProcessingStatus(
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

    private MediaResult toMediaResult(MediaEntity media) {
        MediaResult result = mapper.map(media, MediaResult.class);
        result.setThumbnailUrl(resolveThumbnailUrl(media));
        return result;
    }

    private String resolveThumbnailUrl(MediaEntity media) {
        if (media.getThumbnailStorageKey() == null
                || media.getThumbnailStorageKey().isBlank()
                || media.getStatus() != RecordStatus.ACTIVE
                || media.getProcessingStatus() != MediaProcessingStatus.READY) {
            return null;
        }

        return UriComponentsBuilder.fromPath(appProperties.getMedia().getPublicPath())
                .pathSegment(media.getPublicKey(), "thumbnail")
                .build()
                .encode()
                .toUriString();
    }

    private void requireReadyActiveMedia(MediaEntity media, String label) {
        if (media.getStatus() != RecordStatus.ACTIVE
                || media.getProcessingStatus() != MediaProcessingStatus.READY) {
            throw ExceptionFactory.invalidParam(label + " must be active and ready.");
        }
    }

    private void registerProcessingJob(UUID mediaId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    enqueueProcessingJob(mediaId);
                } catch (RuntimeException ex) {
                    log.error("Failed to enqueue media processing job [{}]", mediaId, ex);
                }
            }
        });
    }

    private void enqueueProcessingJob(UUID mediaId) {
        jobScheduler.<MediaProcessingJob>enqueue(
                job -> job.execute(mediaId, JobContext.Null));
    }

    private void deleteMediaEntity(MediaEntity media) {
        mediaRepo.delete(media);
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
