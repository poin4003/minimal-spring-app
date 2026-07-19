package com.app.features.media.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.settings.AppProperties;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaProcessingLeaseEntity;
import com.app.features.media.repository.MediaProcessingLeaseRepository;
import com.app.features.media.service.MediaProcessingLeaseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaProcessingLeaseServiceImpl implements MediaProcessingLeaseService {

    private static final long LEASE_GRACE_MINUTES = 5;

    private final MediaProcessingLeaseRepository mediaRepo;
    private final AppProperties appProperties;

    @Transactional
    @Override
    public boolean acquire(UUID mediaId, UUID executionId) {
        MediaProcessingLeaseEntity lease = mediaRepo.findById(mediaId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "Media processing lease: " + mediaId));

        LocalDateTime now = LocalDateTime.now();
        boolean heldByAnotherExecution = lease.getExecutionId() != null
                && !lease.getExecutionId().equals(executionId)
                && lease.getExpiresAt() != null
                && lease.getExpiresAt().isAfter(now);
        if (heldByAnotherExecution) {
            return false;
        }

        long processTimeoutMinutes = appProperties.getMedia()
                .getFfmpeg()
                .getProcessTimeoutMinutes();
        lease.setExecutionId(executionId);
        lease.setExpiresAt(now.plusMinutes(
                processTimeoutMinutes + LEASE_GRACE_MINUTES));
        return true;
    }

    @Transactional
    @Override
    public void release(UUID mediaId, UUID executionId) {
        mediaRepo.findById(mediaId).ifPresent(lease -> {
            if (executionId.equals(lease.getExecutionId())) {
                lease.setExecutionId(null);
                lease.setExpiresAt(null);
            }
        });
    }
}
