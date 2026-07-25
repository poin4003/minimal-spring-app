package com.app.features.notification.event.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.features.notification.event.NotificationCreatedEvent;
import com.app.features.notification.service.NotificationSseService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationSseEventHandler {

    private final NotificationSseService notificationSseSvc;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        notificationSseSvc.signalChanged(event.recipientId());
    }
}
