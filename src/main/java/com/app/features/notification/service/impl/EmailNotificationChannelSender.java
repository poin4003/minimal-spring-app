package com.app.features.notification.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.app.features.email.schema.payload.EmailPayload;
import com.app.features.email.service.EmailService;
import com.app.features.notification.enums.NotificationChannel;
import com.app.features.notification.schema.model.NotificationDeliveryMessage;
import com.app.features.notification.service.NotificationChannelSender;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.notification.email",
        name = "enabled",
        havingValue = "true")
public class EmailNotificationChannelSender
        implements NotificationChannelSender {

    private final EmailService emailSvc;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public String send(NotificationDeliveryMessage message) {
        EmailPayload payload = new EmailPayload();
        payload.setRecipientEmail(message.getRecipientAddress());
        payload.setSubject(message.getSubject());
        payload.setContent(message.getContent());
        payload.setHtml(true);

        return emailSvc.send(payload);
    }
}
