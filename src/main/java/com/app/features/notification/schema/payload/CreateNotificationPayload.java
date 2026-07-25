package com.app.features.notification.schema.payload;

import java.util.UUID;

import com.app.features.notification.enums.NotificationResourceType;
import com.app.features.notification.enums.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateNotificationPayload {

    @NotNull
    private UUID recipientId;

    private UUID actorId;

    @NotNull
    private NotificationType type;

    @NotNull
    private NotificationResourceType resourceType;

    @NotNull
    private UUID resourceId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String content;
}
