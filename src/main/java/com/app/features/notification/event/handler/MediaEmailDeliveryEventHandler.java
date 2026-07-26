package com.app.features.notification.event.handler;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.features.notification.enums.NotificationResourceType;
import com.app.features.notification.enums.NotificationType;
import com.app.features.notification.event.NotificationCreatedEvent;
import com.app.features.notification.service.NotificationEmailDeliveryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.notification.email",
        name = "enabled",
        havingValue = "true")
public class MediaEmailDeliveryEventHandler {

    private static final Set<NotificationType> EMAIL_TYPES = EnumSet.of(
            NotificationType.MEDIA_READY,
            NotificationType.MEDIA_PROCESSING_FAILED);

    private final NotificationEmailDeliveryService
            notificationEmailDeliverySvc;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        if (event.resourceType() != NotificationResourceType.MEDIA
                || !EMAIL_TYPES.contains(event.type())) {
            return;
        }

        try {
            notificationEmailDeliverySvc.createDeliveryIfEnabled(
                    event.notificationId(),
                    event.recipientId());
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to create Email delivery for notification [{}].",
                    event.notificationId(),
                    exception);
        }
    }
}
