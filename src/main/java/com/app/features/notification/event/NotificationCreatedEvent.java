package com.app.features.notification.event;

import java.util.UUID;

import com.app.features.notification.enums.NotificationResourceType;

public record NotificationCreatedEvent(
        UUID recipientId,
        NotificationResourceType resourceType) {
}
