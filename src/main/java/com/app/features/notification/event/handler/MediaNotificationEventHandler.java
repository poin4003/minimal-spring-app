package com.app.features.notification.event.handler;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.features.media.event.MediaDeletedEvent;
import com.app.features.media.event.MediaProcessingFailedEvent;
import com.app.features.media.event.MediaReadyEvent;
import com.app.features.media.event.MediaUploadedEvent;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.notification.enums.NotificationResourceType;
import com.app.features.notification.enums.NotificationType;
import com.app.features.notification.schema.payload.CreateNotificationPayload;
import com.app.features.notification.schema.payload.NotificationTextPayload;
import com.app.features.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaNotificationEventHandler {

    private final NotificationService notificationSvc;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMediaUploaded(MediaUploadedEvent event) {
        boolean requiresProcessing =
                event.processingStatus() == MediaProcessingStatus.PENDING;

        CreateNotificationPayload payload = new CreateNotificationPayload();
        payload.setRecipientId(event.recipientId());
        payload.setType(NotificationType.MEDIA_UPLOADED);
        payload.setResourceType(NotificationResourceType.MEDIA);
        payload.setResourceId(event.mediaId());

        NotificationTextPayload text = new NotificationTextPayload();
        text.setTitleKey("notification.media.uploaded.title");
        text.setContentKey(requiresProcessing
                ? "notification.media.uploaded.processing"
                : "notification.media.uploaded.ready");
        text.setContentArguments(List.of(event.originalName()));
        payload.setText(text);

        createNotificationSafely(payload, event.mediaId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMediaReady(MediaReadyEvent event) {
        CreateNotificationPayload payload = new CreateNotificationPayload();
        payload.setRecipientId(event.recipientId());
        payload.setType(NotificationType.MEDIA_READY);
        payload.setResourceType(NotificationResourceType.MEDIA);
        payload.setResourceId(event.mediaId());

        NotificationTextPayload text = new NotificationTextPayload();
        text.setTitleKey("notification.media.ready.title");
        text.setContentKey("notification.media.ready.content");
        text.setContentArguments(List.of(event.originalName()));
        payload.setText(text);

        replaceUploadedNotificationSafely(payload, event.mediaId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMediaDeleted(MediaDeletedEvent event) {
        try {
            notificationSvc.deleteResourceNotifications(
                    NotificationResourceType.MEDIA,
                    event.mediaId());
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to delete notifications for media [{}].",
                    event.mediaId(),
                    exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMediaProcessingFailed(
            MediaProcessingFailedEvent event) {
        CreateNotificationPayload payload = new CreateNotificationPayload();
        payload.setRecipientId(event.recipientId());
        payload.setType(NotificationType.MEDIA_PROCESSING_FAILED);
        payload.setResourceType(NotificationResourceType.MEDIA);
        payload.setResourceId(event.mediaId());

        NotificationTextPayload text = new NotificationTextPayload();
        text.setTitleKey("notification.media.failed.title");
        text.setContentKey("notification.media.failed.content");
        text.setContentArguments(List.of(event.originalName()));
        payload.setText(text);

        replaceUploadedNotificationSafely(payload, event.mediaId());
    }

    private void createNotificationSafely(
            CreateNotificationPayload payload,
            UUID mediaId) {
        try {
            notificationSvc.createNotificationIfAbsent(payload);
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to create media notification [{}].",
                    mediaId,
                    exception);
        }
    }

    private void replaceUploadedNotificationSafely(
            CreateNotificationPayload payload,
            UUID mediaId) {
        try {
            notificationSvc.replaceNotification(
                    payload,
                    NotificationType.MEDIA_UPLOADED);
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to replace media notification [{}].",
                    mediaId,
                    exception);
        }
    }
}
