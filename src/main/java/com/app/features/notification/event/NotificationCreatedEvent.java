package com.app.features.notification.event;

import java.util.UUID;

public record NotificationCreatedEvent(UUID recipientId) {
}
