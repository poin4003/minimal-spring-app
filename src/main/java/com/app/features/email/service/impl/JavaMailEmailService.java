package com.app.features.email.service.impl;

import java.nio.charset.StandardCharsets;

import org.springframework.mail.MailException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.core.exception.ExceptionFactory;
import com.app.features.email.schema.payload.EmailPayload;
import com.app.features.email.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(
                    appProperties.getNotification()
                            .getEmail()
                            .getFromAddress());
            helper.setTo(payload.getRecipientEmail());
            helper.setSubject(payload.getSubject());
            helper.setText(payload.getContent(), payload.isHtml());

            mailSender.send(message);
            return message.getMessageID();
        } catch (MessagingException | MailException exception) {
            throw ExceptionFactory.serverError(
                    "Unable to send Email.",
                    exception);
        }
    }
}
