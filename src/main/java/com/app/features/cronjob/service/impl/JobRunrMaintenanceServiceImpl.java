package com.app.features.cronjob.service.impl;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.app.config.jobrunr.JobRunrProperties;
import com.app.features.cronjob.repository.JobRunrJobRepository;
import com.app.features.cronjob.service.JobRunrMaintenanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobRunrMaintenanceServiceImpl implements JobRunrMaintenanceService {

    private final JobRunrJobRepository jobRunrJobRepo;
    private final JobRunrProperties jobRunrProperties;

    @Override
    public int cleanupExpiredFailedJobs() {
        Duration retention = jobRunrProperties.getMaintenance().getFailedJobRetention();
        Instant cutoff = Instant.now().minus(retention);

        int deletedCount = jobRunrJobRepo.deleteFailedJobsUpdatedBefore(cutoff);

        log.info(
                "Deleted [{}] failed JobRunr jobs updated before [{}].",
                deletedCount,
                cutoff);

        return deletedCount;
    }
}
