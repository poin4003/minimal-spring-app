package com.app.features.cronjob.job;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.cronjob.annotation.AppRecurringJob;
import com.app.features.cronjob.scheduler.JobHandler;
import com.app.features.cronjob.service.JobRunrMaintenanceService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@AppRecurringJob(
        id = "CLEANUP_JOBRUNR_FAILED_JOBS",
        name = "Cleanup Failed JobRunr Jobs",
        defaultCron = "0 30 2 * * *")
public class CleanupFailedJobRunrJobsJob implements JobHandler {

    private final JobRunrMaintenanceService jobRunrMaintenanceSvc;

    @Override
    @Job(name = "Cleanup Failed JobRunr Jobs", retries = 3)
    public void execute() {
        jobRunrMaintenanceSvc.cleanupExpiredFailedJobs();
    }
}
