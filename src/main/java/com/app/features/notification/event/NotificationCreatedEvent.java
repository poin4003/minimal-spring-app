package com.app.features.notification.event;

import java.util.UUID;

import com.app.features.notification.enums.NotificationResourceType;
import com.app.features.notification.enums.NotificationType;

public record NotificationCreatedEvent(
        UUID notificationId,
        UUID recipientId,
        NotificationType type,
        NotificationResourceType resourceType) {
}
