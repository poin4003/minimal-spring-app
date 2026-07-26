package com.app.features.notification.schema.model;

import java.util.UUID;

import com.app.features.notification.enums.NotificationChannel;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationDeliveryMessage {

    private UUID deliveryId;
    private NotificationChannel channel;
    private String recipientAddress;
    private String subject;
    private String content;
}
