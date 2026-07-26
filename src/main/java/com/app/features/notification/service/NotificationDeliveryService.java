package com.app.features.notification.service;

import java.util.UUID;

import com.app.features.notification.schema.payload.CreateNotificationDeliveryPayload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface NotificationDeliveryService {

    UUID createDeliveryIfAbsent(
            @NotNull @Valid CreateNotificationDeliveryPayload payload);

    void processDelivery(@NotNull UUID deliveryId);
}
