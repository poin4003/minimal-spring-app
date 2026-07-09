package com.app.features.auth.cronjob;

import org.springframework.stereotype.Component;

import com.app.features.auth.service.RefreshTokenService;
import com.app.features.cronjob.annotation.CronJobDef;
import com.app.features.cronjob.scheduler.JobHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CronJobDef(
    jobName = "Cleanup Expired Refresh Tokens",
    description = "Clean up expired refresh tokens"
)
public class CleanupExpiredTokenJob implements JobHandler {

    private final RefreshTokenService refreshTokenService;

    @Override
    public String getSupportedJobType() {
        return "CLEANUP_EXPIRED_TOKENS";
    }

    @Override
    public void execute() {
        refreshTokenService.cleanupExpiredConsumedTokens();
    }
}
