package com.app.features.notification.schema.filter;

import java.util.Objects;
import java.util.UUID;

import com.app.features.notification.enums.NotificationReadState;
import com.app.features.notification.enums.NotificationType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationFilterCriteria {

    private final UUID recipientId;

    private NotificationReadState readState;

    private NotificationType type;

    public NotificationFilterCriteria(UUID recipientId) {
        this.recipientId = Objects.requireNonNull(
                recipientId,
                "Notification recipient ID must not be null.");
    }
}
