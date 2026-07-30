package com.app.features.auth.service;

public interface PasswordResetMaintenanceService {

    int cleanupStalePasswordResets();
}
