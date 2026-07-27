package com.app.features.auth.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.features.auth.repository.ConsumedRefreshTokenRepository;
import com.app.features.auth.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final ConsumedRefreshTokenRepository consumedRefreshTokenRepo;

    @Override
    @Transactional
    public void cleanupExpiredConsumedTokens() {
        Instant now = Instant.now();
        int deleteCount = consumedRefreshTokenRepo.deleteAllExpiredSince(now);

        if (deleteCount > 0) {
            log.info("Deleted [{}] expired consumed refresh tokens.", deleteCount);
        }
    }
}
