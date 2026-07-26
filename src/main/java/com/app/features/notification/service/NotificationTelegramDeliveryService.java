package com.app.features.notification.service;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public interface NotificationTelegramDeliveryService {

    void createDelivery(@NotNull UUID notificationId);
}
