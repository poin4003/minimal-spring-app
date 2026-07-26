package com.app.features.notification.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.app.features.notification.enums.NotificationChannel;
import com.app.features.notification.schema.model.NotificationDeliveryMessage;
import com.app.features.notification.service.NotificationChannelSender;
import com.app.features.telegram.schema.payload.TelegramPayload;
import com.app.features.telegram.service.TelegramService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.notification.telegram",
        name = "enabled",
        havingValue = "true")
public class TelegramNotificationChannelSender
        implements NotificationChannelSender {

    private final TelegramService telegramSvc;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.TELEGRAM;
    }

    @Override
    public String send(NotificationDeliveryMessage message) {
        TelegramPayload payload = new TelegramPayload();
        payload.setChatId(message.getRecipientAddress());
        payload.setContent(message.getContent());

        return telegramSvc.send(payload);
    }
}
