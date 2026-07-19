package com.app.features.media.job;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.cronjob.annotation.AppRecurringJob;
import com.app.features.cronjob.scheduler.JobHandler;
import com.app.features.media.service.MediaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@AppRecurringJob(
        id = "RECOVER_PENDING_MEDIA",
        name = "Recover Pending Media",
        defaultCron = "0 */5 * * * *")
public class RecoverPendingMediaJob implements JobHandler {

    private final MediaService mediaSvc;

    @Override
    @Job(name = "Recover Pending Media", retries = 3)
    public void execute() {
        int recovered = mediaSvc.recoverPendingMedia();
        log.info("Enqueued [{}] pending media recovery jobs.", recovered);
    }
}
