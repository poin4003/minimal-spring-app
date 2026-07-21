package com.app.features.media.job;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.cronjob.annotation.AppRecurringJob;
import com.app.features.cronjob.scheduler.JobHandler;
import com.app.features.media.service.MediaUploadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@AppRecurringJob(
        id = "CLEANUP_MEDIA_UPLOADS",
        name = "Cleanup Media Uploads",
        defaultCron = "0 45 * * * *")
public class CleanupMediaUploadsJob implements JobHandler {

    private final MediaUploadService mediaUploadSvc;

    @Override
    @Job(name = "Cleanup Media Uploads", retries = 3)
    public void execute() {
        int deletedCount = mediaUploadSvc.cleanupExpiredUploads();
        log.info("Deleted [{}] expired media upload sessions.", deletedCount);
    }
}
