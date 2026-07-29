package com.app.features.auth.api.v1.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.i18n.LocaleContextHolder;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimited;
import com.app.core.enums.AppLanguage;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.response.ApiResult;
import com.app.core.utils.HttpUtils;
import com.app.features.auth.schema.payload.LoginPayload;
import com.app.features.auth.schema.payload.RefreshTokenPayload;
import com.app.features.auth.schema.payload.RequestRegistrationOtpPayload;
import com.app.features.auth.schema.result.LoginResult;
import com.app.features.auth.schema.result.RequestRegistrationOtpResult;
import com.app.features.auth.service.AuthService;
import com.app.features.auth.service.RegistrationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private final AuthService authSvc;
    private final RegistrationService registrationSvc;
    private final AppMessageResolver messageResolver;

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
}
