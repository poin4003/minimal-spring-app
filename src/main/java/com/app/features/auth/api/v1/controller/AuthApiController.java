package com.app.features.auth.api.v1.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.context.i18n.LocaleContextHolder;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimited;
import com.app.core.enums.AppLanguage;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.response.ApiResult;
import com.app.core.utils.HttpUtils;
import com.app.features.auth.schema.payload.CompletePasswordResetPayload;
import com.app.features.auth.schema.payload.CompleteRegistrationPayload;
import com.app.features.auth.schema.payload.LoginPayload;
import com.app.features.auth.schema.payload.RefreshTokenPayload;
import com.app.features.auth.schema.payload.RequestPasswordResetOtpPayload;
import com.app.features.auth.schema.payload.RequestRegistrationOtpPayload;
import com.app.features.auth.schema.payload.VerifyPasswordResetOtpPayload;
import com.app.features.auth.schema.payload.VerifyRegistrationOtpPayload;
import com.app.features.auth.schema.result.LoginResult;
import com.app.features.auth.schema.result.RequestPasswordResetOtpResult;
import com.app.features.auth.schema.result.RequestRegistrationOtpResult;
import com.app.features.auth.schema.result.VerifyPasswordResetOtpResult;
import com.app.features.auth.schema.result.VerifyRegistrationOtpResult;
import com.app.features.auth.service.AuthService;
import com.app.features.auth.service.PasswordResetService;
import com.app.features.auth.service.RegistrationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private final AuthService authSvc;
    private final PasswordResetService passwordResetSvc;
    private final RegistrationService registrationSvc;
    private final AppMessageResolver messageResolver;

    @ModelAttribute
    void preventAuthResponseCaching(
            HttpServletResponse response) {
        response.setHeader(
                HttpHeaders.CACHE_CONTROL,
                "no-store, no-cache, max-age=0");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setDateHeader(HttpHeaders.EXPIRES, 0);
    }

    @RateLimited(RateLimitPolicy.AUTH_LOGIN)
    @PostMapping("/login")
    public ApiResult<LoginResult> login(
            @Valid @RequestBody LoginPayload payload,
            HttpServletRequest request) {
        LoginResult result = authSvc.login(payload, HttpUtils.getClientIp(request));
        return ApiResult.ok(
                result,
                messageResolver.get("api.auth.login.success"));
    }

    @RateLimited(RateLimitPolicy.AUTH_REFRESH)
    @PostMapping("/refresh")
    public ApiResult<LoginResult> refreshToken(
            @Valid @RequestBody RefreshTokenPayload payload) {
        LoginResult result = authSvc.refreshToken(payload);
        return ApiResult.ok(
                result,
                messageResolver.get("api.auth.refresh.success"));
    }

    @RateLimited(RateLimitPolicy.REGISTRATION_OTP_IP)
    @PostMapping("/register/request-otp")
    public ApiResult<RequestRegistrationOtpResult> requestRegistrationOtp(
            @Valid @RequestBody RequestRegistrationOtpPayload payload) {
        AppLanguage language = AppLanguage
                .fromLocale(LocaleContextHolder.getLocale())
                .orElse(AppLanguage.EN);

        RequestRegistrationOtpResult result =
                registrationSvc.requestOtp(payload, language);

        return ApiResult.ok(
                result,
                messageResolver.get("api.registration.otpSent"));
    }

    @RateLimited(RateLimitPolicy.REGISTRATION_VERIFY_IP)
    @PostMapping("/register/verify-otp")
    public ApiResult<VerifyRegistrationOtpResult> verifyRegistrationOtp(
            @Valid @RequestBody VerifyRegistrationOtpPayload payload) {
        VerifyRegistrationOtpResult result =
                registrationSvc.verifyOtp(payload);

        return ApiResult.ok(
                result,
                messageResolver.get("api.registration.otpVerified"));
    }

    @RateLimited(RateLimitPolicy.REGISTRATION_COMPLETE_IP)
    @PostMapping("/register/complete")
    public ApiResult<LoginResult> completeRegistration(
            @Valid @RequestBody CompleteRegistrationPayload payload,
            HttpServletRequest request) {
        LoginResult result = registrationSvc.completeRegistration(
                payload,
                HttpUtils.getClientIp(request));

        return ApiResult.ok(
                result,
                messageResolver.get("api.registration.completed"));
    }

    @RateLimited(RateLimitPolicy.PASSWORD_RESET_OTP_IP)
    @PostMapping("/password-reset/request-otp")
    public ApiResult<RequestPasswordResetOtpResult> requestPasswordResetOtp(
            @Valid @RequestBody RequestPasswordResetOtpPayload payload) {
        return ApiResult.ok(
                passwordResetSvc.requestOtp(payload),
                messageResolver.get("api.passwordReset.otpRequested"));
    }

    @RateLimited(RateLimitPolicy.PASSWORD_RESET_VERIFY_IP)
    @PostMapping("/password-reset/verify-otp")
    public ApiResult<VerifyPasswordResetOtpResult> verifyPasswordResetOtp(
            @Valid @RequestBody VerifyPasswordResetOtpPayload payload) {
        return ApiResult.ok(
                passwordResetSvc.verifyOtp(payload),
                messageResolver.get("api.passwordReset.otpVerified"));
    }

    @RateLimited(RateLimitPolicy.PASSWORD_RESET_COMPLETE_IP)
    @PostMapping("/password-reset/complete")
    public ApiResult<Void> completePasswordReset(
            @Valid @RequestBody CompletePasswordResetPayload payload) {
        passwordResetSvc.completePasswordReset(payload);
        return ApiResult.ok(
                null,
                messageResolver.get("api.passwordReset.completed"));
    }
}
