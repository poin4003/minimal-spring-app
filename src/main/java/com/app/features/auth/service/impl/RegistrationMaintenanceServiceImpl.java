package com.app.features.auth.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.settings.AppProperties;
import com.app.features.auth.repository.RegistrationRepository;
import com.app.features.auth.service.RegistrationMaintenanceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationMaintenanceServiceImpl
        implements RegistrationMaintenanceService {

    private final RegistrationRepository registrationRepo;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public int cleanupStaleRegistrations() {
        Duration retention = appProperties.getAuth()
                .getRegistration()
                .getCleanupRetention();
        LocalDateTime cutoff = LocalDateTime.now().minus(retention);

        return registrationRepo.deleteStaleRegistrations(cutoff);
    }
}
