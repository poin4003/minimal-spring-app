package com.app.features.auth.api.v1.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimited;
import com.app.core.response.ApiResult;
import com.app.core.utils.HttpUtils;
import com.app.features.auth.schema.payload.LoginPayload;
import com.app.features.auth.schema.payload.RefreshTokenPayload;
import com.app.features.auth.schema.result.LoginResult;
import com.app.features.auth.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private final AuthService authSvc;

    @RateLimited(RateLimitPolicy.AUTH_LOGIN)
    @PostMapping("/login")
    public ApiResult<LoginResult> login(
            @Valid @RequestBody LoginPayload payload,
            HttpServletRequest request) {
        LoginResult result = authSvc.login(payload, HttpUtils.getClientIp(request));
        return ApiResult.ok(result, "Login successful.");
    }

    @RateLimited(RateLimitPolicy.AUTH_REFRESH)
    @PostMapping("/refresh")
    public ApiResult<LoginResult> refreshToken(
            @Valid @RequestBody RefreshTokenPayload payload) {
        LoginResult result = authSvc.refreshToken(payload);
        return ApiResult.ok(result, "Token refreshed successfully.");
    }
}
