package com.app.features.auth.service.impl;

import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.core.enums.AppLanguage;
import com.app.core.exception.ExceptionFactory;
import com.app.core.i18n.AppMessageResolver;
import com.app.features.auth.schema.model.PasswordResetOtpEmailModel;
import com.app.features.auth.service.PasswordResetEmailService;
import com.app.features.email.enums.EmailTemplate;
import com.app.features.email.schema.payload.EmailPayload;
import com.app.features.email.service.EmailService;
import com.app.features.email.service.EmailTemplateService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class PasswordResetEmailServiceImpl
        implements PasswordResetEmailService {

    private final ObjectProvider<EmailService> emailSvcProvider;
    private final EmailTemplateService emailTemplateSvc;
    private final AppMessageResolver messageResolver;

    @Override
    public void sendOtp(
            String recipientEmail,
            String code,
            AppLanguage language,
            Duration otpTtl) {
        EmailService emailSvc = emailSvcProvider.getIfAvailable();
        if (emailSvc == null) {
            throw ExceptionFactory.serverError(
                    "error.email.senderNotConfigured");
        }

        Locale locale = language.toLocale();
        long expiryMinutes = Math.max(
                1,
                Math.ceilDiv(otpTtl.toSeconds(), 60));

        PasswordResetOtpEmailModel model =
                new PasswordResetOtpEmailModel(
                        messageResolver.get(
                                locale,
                                "email.passwordReset.otp.heading"),
                        messageResolver.get(
                                locale,
                                "email.passwordReset.otp.instruction"),
                        code,
                        messageResolver.get(
                                locale,
                                "email.passwordReset.otp.expiry",
                                expiryMinutes));

        EmailPayload payload = new EmailPayload();
        payload.setRecipientEmail(recipientEmail);
        payload.setSubject(messageResolver.get(
                locale,
                "email.passwordReset.otp.subject"));
        payload.setContent(emailTemplateSvc.render(
                EmailTemplate.PASSWORD_RESET_OTP,
                model));
        payload.setHtml(true);

        emailSvc.send(payload);
    }
}
