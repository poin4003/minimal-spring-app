package com.app.features.notification.service;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public interface NotificationPolicyService {

    long enforceHardLimit(@NotNull UUID recipientId);

    long deleteExpiredNotifications();

    long deleteOverflowNotifications();
}
