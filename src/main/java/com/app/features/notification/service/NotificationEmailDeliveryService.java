package com.app.features.notification.service;

import java.util.UUID;

import com.app.features.email.enums.EmailTemplate;

import jakarta.validation.constraints.NotNull;

public interface NotificationEmailDeliveryService {

    void createDeliveryIfEnabled(
            @NotNull UUID notificationId,
            @NotNull UUID recipientId,
            @NotNull EmailTemplate template);
}
