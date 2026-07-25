package com.app.features.notification.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.notification.schema.filter.NotificationFilterCriteria;
import com.app.features.notification.schema.payload.CreateNotificationPayload;
import com.app.features.notification.schema.result.NotificationResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface NotificationService {

    NotificationResult createNotification(
            @NotNull @Valid CreateNotificationPayload payload);

    NotificationResult createNotificationIfAbsent(
            @NotNull @Valid CreateNotificationPayload payload);

    Page<NotificationResult> getManyNotifications(
            @NotNull NotificationFilterCriteria criteria,
            @NotNull Pageable pageable);

    NotificationResult getNotification(
            @NotNull UUID recipientId,
            @NotNull UUID notificationId);

    NotificationResult markAsRead(
            @NotNull UUID recipientId,
            @NotNull UUID notificationId);

    int markAllAsRead(@NotNull UUID recipientId);

    long countUnreadNotifications(@NotNull UUID recipientId);
}
