package com.app.features.auth.service.impl;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimitService;
import com.app.config.settings.AppProperties;
import com.app.core.constant.DefaultRoleConstants;
import com.app.core.enums.AppLanguage;
import com.app.core.exception.ExceptionFactory;
import com.app.features.auth.entity.RegistrationEntity;
import com.app.features.auth.enums.OtpVerificationStatus;
import com.app.features.auth.repository.RegistrationRepository;
import com.app.features.auth.schema.model.OtpVerificationDecision;
import com.app.features.auth.schema.model.RegistrationOtpIssue;
import com.app.features.auth.schema.payload.CompleteRegistrationPayload;
import com.app.features.auth.schema.payload.RequestRegistrationOtpPayload;
import com.app.features.auth.schema.payload.VerifyRegistrationOtpPayload;
import com.app.features.auth.schema.result.LoginResult;
import com.app.features.auth.schema.result.RequestRegistrationOtpResult;
import com.app.features.auth.schema.result.VerifyRegistrationOtpResult;
import com.app.features.auth.security.RegistrationCredentialCodec;
import com.app.features.auth.service.AuthService;
import com.app.features.auth.service.RegistrationEmailService;
import com.app.features.auth.service.RegistrationService;
import com.app.features.notification.entity.UserNotificationPreferenceEntity;
import com.app.features.notification.repository.UserNotificationPreferenceRepository;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.repository.RoleRepository;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.enums.UserStatusEnum;
import com.app.features.user.repository.UserBaseRepository;
import com.app.features.user.repository.UserInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepo;
    private final RoleRepository roleRepo;
    private final UserBaseRepository userBaseRepo;
    private final UserInfoRepository userInfoRepo;
    private final UserNotificationPreferenceRepository
            notificationPreferenceRepo;
    private final RegistrationCredentialCodec credentialCodec;
    private final RegistrationEmailService registrationEmailSvc;
    private final AuthService authSvc;
    private final RateLimitService rateLimitSvc;
    private final AppProperties appProperties;
    private final PasswordEncoder passwordEncoder;

    @Override
    public RequestRegistrationOtpResult requestOtp(
            RequestRegistrationOtpPayload payload,
            AppLanguage language) {
        String email = normalizeEmail(payload.getEmail());

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

    @Override
    public VerifyRegistrationOtpResult verifyOtp(
            VerifyRegistrationOtpPayload payload) {
        String email = normalizeEmail(payload.getEmail());
        enforceVerificationEmailRateLimit(email);

        String completionToken =
                credentialCodec.generateCompletionToken();
        String completionTokenHash =
                credentialCodec.hash(completionToken);

        OtpVerificationDecision decision = verifyOtpState(
                email,
                payload.getCode(),
                completionTokenHash);

        if (decision.getStatus() == OtpVerificationStatus.INVALID) {
            throw ExceptionFactory.invalidParam(
                    "error.registration.otpInvalid");
        }
        if (decision.getStatus() == OtpVerificationStatus.EXPIRED) {
            throw ExceptionFactory.invalidParam(
                    "error.registration.otpExpired");
        }
        if (decision.getStatus()
                == OtpVerificationStatus.ATTEMPTS_EXHAUSTED) {
            throw ExceptionFactory.invalidParam(
                    "error.registration.otpAttemptsExceeded");
        }

        VerifyRegistrationOtpResult result =
                new VerifyRegistrationOtpResult();
        result.setCompletionToken(completionToken);
        result.setExpiresAt(decision.getCompletionExpiresAt());
        return result;
    }

    @Override
    public LoginResult completeRegistration(
            CompleteRegistrationPayload payload,
            String ipAddress) {
        String completionTokenHash =
                credentialCodec.hash(payload.getCompletionToken());

        try {
            return provisionAccount(
                    completionTokenHash,
                    payload.getPassword(),
                    ipAddress);
        } catch (DataIntegrityViolationException exception) {
            throw ExceptionFactory.alreadyExists(
                    "error.registration.emailAlreadyRegistered");
        }
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
    private OtpVerificationDecision verifyOtpState(
            String email,
            String rawCode,
            String completionTokenHash) {
        RegistrationEntity registration = registrationRepo
                .findByEmail(email)
                .orElse(null);

        if (registration == null
                || registration.getCodeHash() == null) {
            return new OtpVerificationDecision(
                    OtpVerificationStatus.INVALID,
                    null);
        }

        Instant now = Instant.now();
        if (registration.getOtpExpiresAt() == null
                || !registration.getOtpExpiresAt().isAfter(now)) {
            registration.setCodeHash(null);
            registration.setOtpExpiresAt(null);
            return new OtpVerificationDecision(
                    OtpVerificationStatus.EXPIRED,
                    null);
        }

        int maxAttempts = appProperties.getAuth()
                .getRegistration()
                .getMaxAttempts();
        if (registration.getFailedAttempts() >= maxAttempts) {
            return new OtpVerificationDecision(
                    OtpVerificationStatus.ATTEMPTS_EXHAUSTED,
                    null);
        }

        if (!credentialCodec.matches(
                rawCode,
                registration.getCodeHash())) {
            registration.setFailedAttempts(
                    registration.getFailedAttempts() + 1);
            OtpVerificationStatus status =
                    registration.getFailedAttempts() >= maxAttempts
                            ? OtpVerificationStatus.ATTEMPTS_EXHAUSTED
                            : OtpVerificationStatus.INVALID;
            return new OtpVerificationDecision(status, null);
        }

        Instant completionExpiresAt = now.plus(
                appProperties.getAuth()
                        .getRegistration()
                        .getCompletionTokenTtl());
        registration.setCodeHash(null);
        registration.setOtpExpiresAt(null);
        registration.setResendAvailableAt(null);
        registration.setFailedAttempts(0);
        registration.setVerifiedAt(now);
        registration.setCompletionTokenHash(completionTokenHash);
        registration.setCompletionExpiresAt(completionExpiresAt);

        return new OtpVerificationDecision(
                OtpVerificationStatus.VERIFIED,
                completionExpiresAt);
    }

    @Transactional
    private LoginResult provisionAccount(
            String completionTokenHash,
            String password,
            String ipAddress) {
        RegistrationEntity registration = registrationRepo
                .findByCompletionTokenHash(completionTokenHash)
                .orElseThrow(() -> ExceptionFactory.invalidToken(
                        "error.registration.completionTokenInvalid"));

        Instant now = Instant.now();
        if (registration.getCompletedAt() != null
                || registration.getVerifiedAt() == null
                || registration.getCompletionExpiresAt() == null
                || !registration.getCompletionExpiresAt().isAfter(now)) {
            throw ExceptionFactory.invalidToken(
                    "error.registration.completionTokenInvalid");
        }

        if (userBaseRepo.existsByEmail(registration.getEmail())) {
            throw ExceptionFactory.alreadyExists(
                    "email",
                    registration.getEmail(),
                    "error.registration.emailAlreadyRegistered");
        }

        RoleEntity defaultRole = roleRepo
                .findByKey(DefaultRoleConstants.USER)
                .orElseThrow(() -> ExceptionFactory.serverError(
                        "error.registration.defaultRoleMissing"));

        UserBaseEntity user = new UserBaseEntity();
        user.setEmail(registration.getEmail());
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(UserStatusEnum.ACTIVE);
        HashSet<RoleEntity> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);
        user = userBaseRepo.save(user);

        UserInfoEntity userInfo = new UserInfoEntity();
        userInfo.setUser(user);
        userInfo.setLanguage(registration.getLanguage());
        userInfoRepo.save(userInfo);

        UserNotificationPreferenceEntity preference =
                new UserNotificationPreferenceEntity();
        preference.setUser(user);
        preference.setEmailEnabled(false);
        notificationPreferenceRepo.save(preference);

        registration.setCompletedAt(now);
        registration.setCompletionTokenHash(null);
        registration.setCompletionExpiresAt(null);

        return authSvc.openSession(user.getId(), ipAddress);
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
            UUID registrationId,
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

    private void enforceVerificationEmailRateLimit(String email) {
        long retryAfterSeconds = rateLimitSvc.consume(
                RateLimitPolicy.REGISTRATION_VERIFY_EMAIL,
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
