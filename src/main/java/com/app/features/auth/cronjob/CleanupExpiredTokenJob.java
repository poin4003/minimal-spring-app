package com.app.features.auth.cronjob;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.auth.service.RefreshTokenService;
import com.app.features.cronjob.annotation.AppRecurringJob;
import com.app.features.cronjob.scheduler.JobHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@AppRecurringJob(
        id = "CLEANUP_EXPIRED_TOKENS",
        name = "Cleanup Expired Refresh Tokens",
        defaultCron = "0 0 1 * * *")
public class CleanupExpiredTokenJob implements JobHandler {

    private final RefreshTokenService refreshTokenService;

    @Override
    @Job(name = "Cleanup Expired Refresh Tokens")
    public void execute() {
        refreshTokenService.cleanupExpiredConsumedTokens();
    }
}
