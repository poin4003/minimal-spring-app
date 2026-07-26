package com.app.features.notification.service.impl;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.core.exception.ExceptionFactory;
import com.app.features.notification.entity.NotificationEntity;
import com.app.features.notification.enums.NotificationChannel;
import com.app.features.notification.repository.NotificationRepository;
import com.app.features.notification.schema.payload.CreateNotificationDeliveryPayload;
import com.app.features.notification.service.NotificationDeliveryService;
import com.app.features.notification.service.NotificationTelegramDeliveryService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.notification.telegram",
        name = "enabled",
        havingValue = "true")
public class NotificationTelegramDeliveryServiceImpl
        implements NotificationTelegramDeliveryService {

    private final NotificationRepository notificationRepo;
    private final NotificationDeliveryService notificationDeliverySvc;
    private final AppProperties appProperties;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDelivery(UUID notificationId) {
        NotificationEntity notification = notificationRepo
                .findById(notificationId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "Notification: " + notificationId));

        CreateNotificationDeliveryPayload payload =
                new CreateNotificationDeliveryPayload();
        payload.setNotificationId(notification.getId());
        payload.setChannel(NotificationChannel.TELEGRAM);
        payload.setRecipientAddress(
                appProperties.getNotification()
                        .getTelegram()
                        .getGroupChatId());
        payload.setSubjectSnapshot(notification.getTitle());
        payload.setContentSnapshot(
                notification.getTitle()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + notification.getContent());

        notificationDeliverySvc.createDeliveryIfAbsent(payload);
    }
}
