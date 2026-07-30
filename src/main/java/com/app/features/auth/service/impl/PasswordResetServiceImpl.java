package com.app.features.auth.service.impl;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimitService;
import com.app.config.settings.AppProperties;
import com.app.core.enums.AppLanguage;
import com.app.core.exception.ExceptionFactory;
import com.app.features.auth.entity.PasswordResetEntity;
import com.app.features.auth.repository.PasswordResetRepository;
import com.app.features.auth.schema.model.PasswordResetOtpIssue;
import com.app.features.auth.schema.payload.RequestPasswordResetOtpPayload;
import com.app.features.auth.schema.result.RequestPasswordResetOtpResult;
import com.app.features.auth.security.AuthCredentialCodec;
import com.app.features.auth.service.PasswordResetEmailService;
import com.app.features.auth.service.PasswordResetService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.enums.UserStatusEnum;
import com.app.features.user.repository.UserBaseRepository;
import com.app.features.user.repository.UserInfoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl
        implements PasswordResetService {

    private final PasswordResetRepository passwordResetRepo;
    private final UserBaseRepository userBaseRepo;
    private final UserInfoRepository userInfoRepo;
    private final AuthCredentialCodec credentialCodec;
    private final PasswordResetEmailService passwordResetEmailSvc;
    private final RateLimitService rateLimitSvc;
    private final AppProperties appProperties;

    @Override
    public RequestPasswordResetOtpResult requestOtp(
            RequestPasswordResetOtpPayload payload) {
        String email = normalizeEmail(payload.getEmail());
        enforceEmailRateLimit(email);

        AppProperties.PasswordReset config =
                appProperties.getAuth().getPasswordReset();
        Instant requestedAt = Instant.now();

        RequestPasswordResetOtpResult result =
                new RequestPasswordResetOtpResult();
        result.setExpiresAt(requestedAt.plus(config.getOtpTtl()));
        result.setResendAvailableAt(
                requestedAt.plus(config.getResendCooldown()));

        String rawCode = credentialCodec.generateOtp(
                config.getOtpLength());
        String codeHash = credentialCodec.hash(rawCode);

        Optional<PasswordResetOtpIssue> issue =
                prepareOtp(email, codeHash);

        if (issue.isPresent()) {
            deliverOtp(issue.get(), rawCode, codeHash);
        }

        return result;
    }

    @Transactional
    private Optional<PasswordResetOtpIssue> prepareOtp(
            String email,
            String codeHash) {
        Optional<UserBaseEntity> userOptional =
                userBaseRepo.findOneByEmailAndStatus(
                        email,
                        UserStatusEnum.ACTIVE);

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        UserBaseEntity user = userOptional.get();
        Instant now = Instant.now();

        PasswordResetEntity passwordReset = passwordResetRepo
                .findByUser_Id(user.getId())
                .orElseGet(() -> {
                    PasswordResetEntity entity =
                            new PasswordResetEntity();
                    entity.setUser(user);
                    return entity;
                });

        if (passwordReset.getResendAvailableAt() != null
                && passwordReset.getResendAvailableAt().isAfter(now)) {
            return Optional.empty();
        }

        AppProperties.PasswordReset config =
                appProperties.getAuth().getPasswordReset();
        AppLanguage language = userInfoRepo
                .findLanguageByUserId(user.getId())
                .orElse(AppLanguage.EN);

        passwordReset.setCodeHash(codeHash);
        passwordReset.setFailedAttempts(0);
        passwordReset.setOtpExpiresAt(
                now.plus(config.getOtpTtl()));
        passwordReset.setResendAvailableAt(
                now.plus(config.getResendCooldown()));
        passwordReset.setVerifiedAt(null);
        passwordReset.setResetTokenHash(null);
        passwordReset.setResetTokenExpiresAt(null);
        passwordReset.setCompletedAt(null);

        passwordReset = passwordResetRepo.saveAndFlush(
                passwordReset);

        return Optional.of(new PasswordResetOtpIssue(
                passwordReset.getId(),
                user.getEmail(),
                language,
                passwordReset.getOtpExpiresAt(),
                passwordReset.getResendAvailableAt()));
    }

    private void deliverOtp(
            PasswordResetOtpIssue issue,
            String rawCode,
            String codeHash) {
        try {
            passwordResetEmailSvc.sendOtp(
                    issue.getEmail(),
                    rawCode,
                    issue.getLanguage(),
                    appProperties.getAuth()
                            .getPasswordReset()
                            .getOtpTtl());
        } catch (RuntimeException exception) {
            try {
                releaseFailedOtp(
                        issue.getPasswordResetId(),
                        codeHash);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            log.error(
                    "Password reset OTP delivery failed [{}]",
                    issue.getPasswordResetId(),
                    exception);
        }
    }

    @Transactional
    private void releaseFailedOtp(
            UUID passwordResetId,
            String codeHash) {
        PasswordResetEntity passwordReset = passwordResetRepo
                .findById(passwordResetId)
                .orElse(null);

        if (passwordReset == null
                || !codeHash.equals(passwordReset.getCodeHash())) {
            return;
        }

        passwordReset.setCodeHash(null);
        passwordReset.setOtpExpiresAt(null);
        passwordReset.setResendAvailableAt(Instant.now());
        passwordReset.setFailedAttempts(0);
    }

    private void enforceEmailRateLimit(String email) {
        long retryAfterSeconds = rateLimitSvc.consume(
                RateLimitPolicy.PASSWORD_RESET_OTP_EMAIL,
                email);

        if (retryAfterSeconds > 0) {
            throw ExceptionFactory.rateLimitExceeded(
                    "error.rateLimit.exceeded");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
