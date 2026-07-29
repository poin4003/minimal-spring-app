package com.app.features.auth.service.impl;

import java.time.Instant;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimitService;
import com.app.config.settings.AppProperties;
import com.app.core.enums.AppLanguage;
import com.app.core.exception.ExceptionFactory;
import com.app.features.auth.entity.RegistrationEntity;
import com.app.features.auth.repository.RegistrationRepository;
import com.app.features.auth.schema.model.RegistrationOtpIssue;
import com.app.features.auth.schema.payload.RequestRegistrationOtpPayload;
import com.app.features.auth.schema.result.RequestRegistrationOtpResult;
import com.app.features.auth.security.RegistrationCredentialCodec;
import com.app.features.auth.service.RegistrationEmailService;
import com.app.features.auth.service.RegistrationService;
import com.app.features.user.repository.UserBaseRepository;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepo;
    private final UserBaseRepository userBaseRepo;
    private final RegistrationCredentialCodec credentialCodec;
    private final RegistrationEmailService registrationEmailSvc;
    private final RateLimitService rateLimitSvc;
    private final AppProperties appProperties;

    @Override
    public RequestRegistrationOtpResult requestOtp(
            RequestRegistrationOtpPayload payload,
            AppLanguage language) {
        String email = payload.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        enforceEmailRateLimit(email);

        String rawCode = credentialCodec.generateOtp();
        String codeHash = credentialCodec.hash(rawCode);
        RegistrationOtpIssue issue = prepareOtpWithConcurrentInsertRetry(
                email,
                language,
                codeHash);

        try {
            registrationEmailSvc.sendOtp(
                    issue.getEmail(),
                    rawCode,
                    issue.getLanguage(),
                    appProperties.getAuth()
                            .getRegistration()
                            .getOtpTtl());
        } catch (RuntimeException exception) {
            try {
                releaseFailedOtp(
                        issue.getRegistrationId(),
                        codeHash);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }

        RequestRegistrationOtpResult result =
                new RequestRegistrationOtpResult();
        result.setExpiresAt(issue.getExpiresAt());
        result.setResendAvailableAt(issue.getResendAvailableAt());
        return result;
    }

    private RegistrationOtpIssue prepareOtpWithConcurrentInsertRetry(
            String email,
            AppLanguage language,
            String codeHash) {
        try {
            return prepareOtp(email, language, codeHash);
        } catch (DataIntegrityViolationException exception) {
            return prepareOtp(email, language, codeHash);
        }
    }

    @Transactional
    private RegistrationOtpIssue prepareOtp(
            String email,
            AppLanguage language,
            String codeHash) {
        if (userBaseRepo.existsByEmail(email)) {
            throw ExceptionFactory.alreadyExists(
                    "email",
                    email,
                    "error.registration.emailAlreadyRegistered");
        }

        Instant now = Instant.now();
        RegistrationEntity registration = registrationRepo
                .findByEmail(email)
                .orElseGet(() -> new RegistrationEntity());

        if (registration.getResendAvailableAt() != null
                && registration.getResendAvailableAt().isAfter(now)) {
            throw ExceptionFactory.invalidParam(
                    "error.registration.resendCooldown");
        }

        AppProperties.Registration config =
                appProperties.getAuth().getRegistration();

        registration.setEmail(email);
        registration.setLanguage(language);
        registration.setCodeHash(codeHash);
        registration.setFailedAttempts(0);
        registration.setOtpExpiresAt(now.plus(config.getOtpTtl()));
        registration.setResendAvailableAt(
                now.plus(config.getResendCooldown()));
        registration.setVerifiedAt(null);
        registration.setCompletionTokenHash(null);
        registration.setCompletionExpiresAt(null);
        registration.setCompletedAt(null);

        registration = registrationRepo.saveAndFlush(registration);

        return new RegistrationOtpIssue(
                registration.getId(),
                registration.getEmail(),
                registration.getLanguage(),
                registration.getOtpExpiresAt(),
                registration.getResendAvailableAt());
    }

    @Transactional
    private void releaseFailedOtp(
            java.util.UUID registrationId,
            String codeHash) {
        RegistrationEntity registration = registrationRepo
                .findById(registrationId)
                .orElse(null);

        if (registration == null
                || !codeHash.equals(registration.getCodeHash())) {
            return;
        }

        registration.setCodeHash(null);
        registration.setOtpExpiresAt(null);
        registration.setResendAvailableAt(Instant.now());
        registration.setFailedAttempts(0);
    }

    private void enforceEmailRateLimit(String email) {
        long retryAfterSeconds = rateLimitSvc.consume(
                RateLimitPolicy.REGISTRATION_OTP_EMAIL,
                email);
        if (retryAfterSeconds > 0) {
            throw ExceptionFactory.rateLimitExceeded(
                    "error.rateLimit.exceeded");
        }
    }
}
