package com.app.features.email.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.email.schema.payload.EmailPayload;
import com.app.features.email.service.EmailService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.notification.email",
        name = "enabled",
        havingValue = "true")
public class JavaMailEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Override
    public String send(EmailPayload payload) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(
                appProperties.getNotification()
                        .getEmail()
                        .getFromAddress());
        message.setTo(payload.getRecipientEmail());
        message.setSubject(payload.getSubject());
        message.setText(payload.getContent());

        mailSender.send(message);
        return null;
    }
}
