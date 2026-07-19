package com.app.features.media.job;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.cronjob.annotation.AppRecurringJob;
import com.app.features.cronjob.scheduler.JobHandler;
import com.app.features.media.schema.model.MediaStorageCleanupResult;
import com.app.features.media.service.MediaMaintenanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@AppRecurringJob(
        id = "CLEANUP_MEDIA_STORAGE",
        name = "Cleanup Media Storage",
        defaultCron = "0 30 * * * *")
public class CleanupMediaStorageJob implements JobHandler {

    private final MediaMaintenanceService mediaMaintenanceSvc;

    @Override
    @Job(name = "Cleanup Media Storage", retries = 3)
    public void execute() {
        MediaStorageCleanupResult result = mediaMaintenanceSvc.cleanupStorage();
        log.info(
                "Media storage cleanup: staging [{}], processing [{}], orphan [{}].",
                result.getStagingFilesDeleted(),
                result.getProcessingWorkspacesDeleted(),
                result.getOrphanDirectoriesDeleted());
    }
}
