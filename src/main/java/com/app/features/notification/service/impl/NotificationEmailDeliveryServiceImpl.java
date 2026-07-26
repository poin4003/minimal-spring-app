package com.app.features.notification.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.features.notification.enums.NotificationChannel;
import com.app.features.notification.repository.UserNotificationPreferenceRepository;
import com.app.features.notification.schema.payload.CreateNotificationDeliveryPayload;
import com.app.features.notification.service.NotificationDeliveryService;
import com.app.features.notification.service.NotificationEmailDeliveryService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class NotificationEmailDeliveryServiceImpl
        implements NotificationEmailDeliveryService {

    private final UserNotificationPreferenceRepository
            userNotificationPreferenceRepo;
    private final NotificationDeliveryService notificationDeliverySvc;

    @Override
    public void createDeliveryIfEnabled(
            UUID notificationId,
            UUID recipientId) {
        userNotificationPreferenceRepo
                .findByIdAndEmailEnabledTrue(recipientId)
                .ifPresent(preference -> {
                    CreateNotificationDeliveryPayload payload =
                            new CreateNotificationDeliveryPayload();
                    payload.setNotificationId(notificationId);
                    payload.setChannel(NotificationChannel.EMAIL);
                    payload.setRecipientAddress(
                            preference.getUser().getEmail());

                    notificationDeliverySvc
                            .createDeliveryIfAbsent(payload);
                });
    }
}
