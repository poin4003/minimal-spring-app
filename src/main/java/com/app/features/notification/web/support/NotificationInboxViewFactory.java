package com.app.features.notification.web.support;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.features.notification.entity.NotificationEntity_;
import com.app.features.notification.schema.filter.NotificationFilterCriteria;
import com.app.features.notification.schema.result.NotificationResult;
import com.app.features.notification.service.NotificationService;
import com.app.features.notification.web.view.NotificationInboxItemView;
import com.app.features.notification.web.view.NotificationInboxView;
import com.app.features.notification.web.view.NotificationUnreadCountView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationInboxViewFactory {

    private static final int INBOX_SIZE = 10;

    private final AppProperties appProperties;
    private final NotificationService notificationSvc;

    public NotificationInboxView buildInbox(UUID recipientId) {
        Pageable pageable = PageRequest.of(
                0,
                INBOX_SIZE,
                Sort.by(Sort.Direction.DESC, NotificationEntity_.CREATED_AT));
        NotificationFilterCriteria criteria =
                new NotificationFilterCriteria(recipientId);
        Page<NotificationResult> notificationPage =
                notificationSvc.getManyNotifications(criteria, pageable);

        return NotificationInboxView.builder()
                .unreadCount(notificationSvc.countUnreadNotifications(recipientId))
                .items(notificationPage.stream()
                        .map(notification -> toInboxItem(notification))
                        .toList())
                .markAllReadPath(getNotificationBasePath() + "/read-all")
                .build();
    }

    public NotificationUnreadCountView buildUnreadCount(UUID recipientId) {
        return NotificationUnreadCountView.builder()
                .count(notificationSvc.countUnreadNotifications(recipientId))
                .refreshPath(getNotificationBasePath() + "/unread-count")
                .build();
    }

    private NotificationInboxItemView toInboxItem(
            NotificationResult notification) {
        return NotificationInboxItemView.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .createdAt(notification.getCreatedAt())
                .unread(notification.getReadAt() == null)
                .markReadPath(UriComponentsBuilder
                        .fromPath(getNotificationBasePath())
                        .pathSegment(notification.getId().toString(), "read")
                        .build()
                        .encode()
                        .toUriString())
                .iconClass(resolveIconClass(notification))
                .iconThemeClass(resolveIconThemeClass(notification))
                .build();
    }

    private String resolveIconClass(NotificationResult notification) {
        return switch (notification.getType()) {
            case MEDIA_READY -> "bi-check-circle-fill";
            case MEDIA_PROCESSING_FAILED ->
                "bi-exclamation-triangle-fill";
        };
    }

    private String resolveIconThemeClass(NotificationResult notification) {
        return switch (notification.getType()) {
            case MEDIA_READY ->
                "bg-success-subtle text-success-emphasis";
            case MEDIA_PROCESSING_FAILED ->
                "bg-danger-subtle text-danger-emphasis";
        };
    }

    private String getNotificationBasePath() {
        return appProperties.getUi().getHomePath() + "/notifications";
    }
}
