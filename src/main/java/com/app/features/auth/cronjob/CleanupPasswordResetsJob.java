package com.app.features.auth.cronjob;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.auth.service.PasswordResetMaintenanceService;
import com.app.features.cronjob.annotation.AppRecurringJob;
import com.app.features.cronjob.scheduler.JobHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@AppRecurringJob(
        id = "CLEANUP_PASSWORD_RESETS",
        name = "Cleanup Password Resets",
        defaultCron = "0 45 2 * * *")
public class CleanupPasswordResetsJob implements JobHandler {

    private final PasswordResetMaintenanceService
            passwordResetMaintenanceSvc;

    @Override
    @Job(name = "Cleanup Password Resets", retries = 3)
    public void execute() {
        int deletedCount =
                passwordResetMaintenanceSvc
                        .cleanupStalePasswordResets();

        if (deletedCount > 0) {
            log.info(
                    "Deleted [{}] stale password reset records.",
                    deletedCount);
        }
    }
}
