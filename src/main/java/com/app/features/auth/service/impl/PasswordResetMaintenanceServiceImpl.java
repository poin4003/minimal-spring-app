package com.app.features.auth.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.settings.AppProperties;
import com.app.features.auth.repository.PasswordResetRepository;
import com.app.features.auth.service.PasswordResetMaintenanceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetMaintenanceServiceImpl
        implements PasswordResetMaintenanceService {

    private final PasswordResetRepository passwordResetRepo;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public int cleanupStalePasswordResets() {
        Duration retention = appProperties.getAuth()
                .getPasswordReset()
                .getCleanupRetention();
        LocalDateTime cutoff = LocalDateTime.now().minus(retention);

        return passwordResetRepo.deleteStalePasswordResets(cutoff);
    }
}
