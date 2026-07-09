package com.app.features.auth.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.app.features.auth.repository.ConsumedRefreshTokenRepository;
import com.app.features.auth.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final ConsumedRefreshTokenRepository consumedRefreshTokenRepo;

    // @Scheduled(cron = "0/10 * * * * *")
    @Override
    public void cleanupExpiredConsumedTokens() {
        log.info("Starting cleanup of expired consumed refresh token...");

        Instant now = Instant.now();

        int deleteCount = consumedRefreshTokenRepo.deleteAllExpiredSince(now);

        log.info("Finished cleanup. Deleted {} expired consumed token at {}", deleteCount, now);
    }
}
