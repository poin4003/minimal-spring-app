package com.app.features.notification.schema.result;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.notification.enums.NotificationResourceType;
import com.app.features.notification.enums.NotificationType;
import com.app.features.user.schema.result.UserShortResult;

import lombok.Data;

@Data
public class NotificationResult {

    private UUID id;

    private UserShortResult actor;

    private NotificationType type;

    private NotificationResourceType resourceType;

    private UUID resourceId;

    private String title;

    private String content;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
