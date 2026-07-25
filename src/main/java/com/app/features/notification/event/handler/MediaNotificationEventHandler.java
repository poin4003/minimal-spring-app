package com.app.features.notification.event.handler;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.features.media.event.MediaProcessingFailedEvent;
import com.app.features.media.event.MediaReadyEvent;
import com.app.features.notification.enums.NotificationResourceType;
import com.app.features.notification.enums.NotificationType;
import com.app.features.notification.schema.payload.CreateNotificationPayload;
import com.app.features.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaNotificationEventHandler {

    private final NotificationService notificationSvc;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMediaReady(MediaReadyEvent event) {
        CreateNotificationPayload payload = new CreateNotificationPayload();
        payload.setRecipientId(event.recipientId());
        payload.setType(NotificationType.MEDIA_READY);
        payload.setResourceType(NotificationResourceType.MEDIA);
        payload.setResourceId(event.mediaId());
        payload.setTitle("Media is ready");
        payload.setContent(event.originalName() + " is ready to stream.");

        createNotificationSafely(payload, event.mediaId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMediaProcessingFailed(
            MediaProcessingFailedEvent event) {
        CreateNotificationPayload payload = new CreateNotificationPayload();
        payload.setRecipientId(event.recipientId());
        payload.setType(NotificationType.MEDIA_PROCESSING_FAILED);
        payload.setResourceType(NotificationResourceType.MEDIA);
        payload.setResourceId(event.mediaId());
        payload.setTitle("Media processing failed");
        payload.setContent(
                event.originalName()
                        + " could not be prepared. You can retry processing.");

        createNotificationSafely(payload, event.mediaId());
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
}
