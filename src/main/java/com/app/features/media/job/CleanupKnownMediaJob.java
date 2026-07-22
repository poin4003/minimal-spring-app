package com.app.features.media.job;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.cronjob.annotation.AppRecurringJob;
import com.app.features.cronjob.scheduler.JobHandler;
import com.app.features.media.schema.model.KnownMediaCleanupResult;
import com.app.features.media.service.MediaMaintenanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@AppRecurringJob(
        id = "CLEANUP_KNOWN_MEDIA",
        name = "Cleanup Known Media",
        defaultCron = "0 15 * * * *")
public class CleanupKnownMediaJob implements JobHandler {

    private final MediaMaintenanceService mediaMaintenanceSvc;

    @Override
    @Job(name = "Cleanup Known Media", retries = 3)
    public void execute() {
        KnownMediaCleanupResult result = mediaMaintenanceSvc.cleanupKnownMedia();
        log.info(
                "Known media cleanup completed: derived artifacts cleaned [{}], missing originals [{}].",
                result.getFailedArtifactsCleaned(),
                result.getMissingOriginalsDetected());
    }
}
