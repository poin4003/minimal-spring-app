package com.app.features.media.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

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
import com.app.features.media.schema.model.KnownMediaCleanupResult;
import com.app.features.media.service.MediaMaintenanceService;
import com.app.features.media.service.MediaProcessingLeaseService;
import com.app.features.media.storage.MediaFileStorage;

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
        LocalDateTime failedHlsCutoff = now.minus(config.getFailedHlsTtl());
        LocalDateTime missingOriginalCutoff = now.minus(config.getMissingOriginalAuditTtl());

        int failedHlsCleaned = processCandidates(
                pageable -> mediaRepo.findAllByStatusAndProcessingStatusAndUpdatedAtBefore(
                        RecordStatus.ACTIVE,
                        MediaProcessingStatus.FAILED,
                        failedHlsCutoff,
                        pageable),
                media -> cleanupFailedHls(media.getId(), failedHlsCutoff),
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
                failedHlsCleaned,
                missingOriginalsDetected);
    }

    @Transactional
    private boolean cleanupFailedHls(UUID mediaId, LocalDateTime cutoff) {
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
            long deletedVariants = mediaVariantRepo.deleteAllByMedia_Id(mediaId);
            return deletedHls || deletedVariants > 0;
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
