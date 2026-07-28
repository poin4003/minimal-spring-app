package com.app.features.notification.schema.payload;

import java.util.UUID;

import com.app.features.notification.enums.NotificationResourceType;
import com.app.features.notification.enums.NotificationType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    @Valid
    @NotNull
    private NotificationTextPayload text;
}
