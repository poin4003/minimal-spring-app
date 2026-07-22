package com.app.features.media.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.settings.AppProperties;
import com.app.core.enums.RecordStatus;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.entity.MediaEntity_;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.repository.MediaRepository;
import com.app.features.media.repository.MediaVariantRepository;
import com.app.features.media.repository.spec.MediaSpecification;
import com.app.features.media.schema.model.KnownMediaCleanupResult;
import com.app.features.media.schema.model.MediaStorageCleanupResult;
import com.app.features.media.service.MediaMaintenanceService;
import com.app.features.media.service.MediaProcessingLeaseService;
import com.app.features.media.storage.MediaFileStorage;
import com.app.features.media.storage.MediaStorageKeySupport;
import com.app.features.media.storage.schema.MediaStorageDirectoryCandidate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaMaintenanceServiceImpl implements MediaMaintenanceService {

    private final MediaRepository mediaRepo;
    private final MediaVariantRepository mediaVariantRepo;
    private final MediaProcessingLeaseService mediaProcessingLeaseSvc;
    private final MediaFileStorage mediaFileStorage;
    private final AppProperties appProperties;

    @Override
    public KnownMediaCleanupResult cleanupKnownMedia() {
        AppProperties.MediaMaintenance config = appProperties.getMedia().getMaintenance();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime failedArtifactCutoff = now.minus(config.getFailedArtifactTtl());
        LocalDateTime missingOriginalCutoff = now.minus(config.getMissingOriginalAuditTtl());

        int failedArtifactsCleaned = processCandidates(
                pageable -> mediaRepo.findAllByStatusAndProcessingStatusAndUpdatedAtBefore(
                        RecordStatus.ACTIVE,
                        MediaProcessingStatus.FAILED,
                        failedArtifactCutoff,
                        pageable),
                media -> cleanupFailedArtifacts(media.getId(), failedArtifactCutoff),
                config.getBatchSize());
        int missingOriginalsDetected = processCandidates(
                pageable -> mediaRepo.findAllByStatusAndProcessingStatusNotAndUpdatedAtBefore(
                        RecordStatus.ACTIVE,
                        MediaProcessingStatus.FAILED,
                        missingOriginalCutoff,
                        pageable),
                media -> markMissingOriginalAsFailed(media.getId(), missingOriginalCutoff),
                config.getBatchSize());

        return new KnownMediaCleanupResult(
                failedArtifactsCleaned,
                missingOriginalsDetected);
    }

    @Override
    public MediaStorageCleanupResult cleanupStorage() {
        AppProperties.MediaMaintenance config = appProperties.getMedia().getMaintenance();
        Instant now = Instant.now();

        int stagingFilesDeleted = mediaFileStorage.deleteStagedFilesOlderThan(
                now.minus(config.getStagingTtl()),
                config.getBatchSize());
        int processingWorkspacesDeleted =
                mediaFileStorage.deleteProcessingWorkspacesOlderThan(
                        now.minus(config.getProcessingWorkspaceTtl()),
                        config.getBatchSize());
        int orphanDirectoriesDeleted = cleanupOrphanDirectories(
                now.minus(config.getOrphanDirectoryTtl()),
                config.getBatchSize());

        return new MediaStorageCleanupResult(
                stagingFilesDeleted,
                processingWorkspacesDeleted,
                orphanDirectoriesDeleted);
    }

    private int cleanupOrphanDirectories(Instant cutoff, int batchSize) {
        List<MediaStorageDirectoryCandidate> candidates =
                mediaFileStorage.findMediaDirectoriesOlderThan(cutoff);
        int deleted = 0;

        for (int offset = 0; offset < candidates.size(); offset += batchSize) {
            int end = Math.min(offset + batchSize, candidates.size());
            List<MediaStorageDirectoryCandidate> batch = candidates.subList(offset, end);
            Set<String> candidateKeys = batch.stream()
                    .map(candidate -> candidate.getStorageDirectoryKey())
                    .collect(Collectors.toSet());
            Set<String> referencedKeys = mediaRepo
                    .findAll(MediaSpecification.storageDirectoryIn(candidateKeys))
                    .stream()
                    .map(media -> MediaStorageKeySupport.directoryOf(media.getStorageKey()))
                    .collect(Collectors.toSet());

            for (MediaStorageDirectoryCandidate candidate : batch) {
                if (!referencedKeys.contains(candidate.getStorageDirectoryKey())
                        && mediaFileStorage.deleteMediaDirectory(
                                candidate.getStorageDirectoryKey())) {
                    deleted++;
                }
            }
        }

        return deleted;
    }

    @Transactional
    private boolean cleanupFailedArtifacts(UUID mediaId, LocalDateTime cutoff) {
        UUID executionId = UUID.randomUUID();
        if (!mediaProcessingLeaseSvc.acquire(mediaId, executionId)) {
            return false;
        }

        try {
            MediaEntity media = mediaRepo.findById(mediaId).orElse(null);
            if (!isFailedBefore(media, cutoff)) {
                return false;
            }

            boolean deletedHls = mediaFileStorage.deleteHlsArtifacts(media.getStorageKey());
            boolean hasPersistedThumbnail = media.getThumbnailStorageKey() != null
                    && !media.getThumbnailStorageKey().isBlank();
            boolean deletedThumbnail = !hasPersistedThumbnail
                    && mediaFileStorage.deleteThumbnailArtifact(media.getStorageKey());
            long deletedVariants = mediaVariantRepo.deleteAllByMedia_Id(mediaId);
            return deletedHls || deletedThumbnail || deletedVariants > 0;
        } finally {
            mediaProcessingLeaseSvc.release(mediaId, executionId);
        }
    }

    @Transactional
    private boolean markMissingOriginalAsFailed(UUID mediaId, LocalDateTime cutoff) {
        UUID executionId = UUID.randomUUID();
        if (!mediaProcessingLeaseSvc.acquire(mediaId, executionId)) {
            return false;
        }

        try {
            MediaEntity media = mediaRepo.findById(mediaId).orElse(null);
            if (!isAuditable(media, cutoff)
                    || mediaFileStorage.exists(media.getStorageKey())) {
                return false;
            }

            media.setProcessingStatus(MediaProcessingStatus.FAILED);
            log.warn("Original file is missing for media [{}].", mediaId);
            return true;
        } finally {
            mediaProcessingLeaseSvc.release(mediaId, executionId);
        }
    }

    private int processCandidates(
            Function<Pageable, Page<MediaEntity>> pageLoader,
            Predicate<MediaEntity> processor,
            int batchSize) {
        Sort sort = Sort.by(
                Sort.Direction.ASC,
                MediaEntity_.UPDATED_AT,
                MediaEntity_.ID);
        Page<MediaEntity> firstPage = pageLoader.apply(PageRequest.of(0, batchSize, sort));
        int processed = 0;

        for (int pageNumber = firstPage.getTotalPages() - 1;
                pageNumber >= 0;
                pageNumber--) {
            Page<MediaEntity> candidates = pageNumber == 0
                    ? firstPage
                    : pageLoader.apply(PageRequest.of(pageNumber, batchSize, sort));

            for (MediaEntity media : candidates.getContent()) {
                try {
                    if (processor.test(media)) {
                        processed++;
                    }
                } catch (RuntimeException ex) {
                    log.error("Failed to maintain media [{}].", media.getId(), ex);
                }
            }
        }

        return processed;
    }

    private boolean isFailedBefore(MediaEntity media, LocalDateTime cutoff) {
        return media != null
                && media.getStatus() == RecordStatus.ACTIVE
                && media.getProcessingStatus() == MediaProcessingStatus.FAILED
                && media.getUpdatedAt() != null
                && media.getUpdatedAt().isBefore(cutoff);
    }

    private boolean isAuditable(MediaEntity media, LocalDateTime cutoff) {
        return media != null
                && media.getStatus() == RecordStatus.ACTIVE
                && media.getProcessingStatus() != MediaProcessingStatus.FAILED
                && media.getUpdatedAt() != null
                && media.getUpdatedAt().isBefore(cutoff);
    }
}
