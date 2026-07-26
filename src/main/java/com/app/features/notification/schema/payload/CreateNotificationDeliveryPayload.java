package com.app.features.notification.schema.payload;

import java.util.UUID;

import com.app.features.notification.enums.NotificationChannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateNotificationDeliveryPayload {

    @NotNull
    private UUID notificationId;

    @NotNull
    private NotificationChannel channel;

    @NotBlank
    @Size(max = 512)
    private String recipientAddress;
}
