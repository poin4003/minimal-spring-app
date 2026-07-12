package com.app.features.auth.api.v1.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.core.annotation.ClientIp;
import com.app.core.response.ApiResult;
import com.app.core.security.UserPrincipal;
import com.app.features.auth.schema.payload.LoginPayload;
import com.app.features.auth.schema.payload.RefreshTokenPayload;
import com.app.features.auth.schema.result.LoginResult;
import com.app.features.auth.service.AuthService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication V1", description = "Auth docs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResult<LoginResult> login(
            @Valid @RequestBody LoginPayload req,
            @ClientIp String ipAddress) {
        LoginResult result = authService.login(req, ipAddress);
        return ApiResult.ok(result, "Login success");
    }

    @PostMapping("/refresh")
    public ApiResult<LoginResult> refreshToken(
            @RequestBody RefreshTokenPayload req) {

        LoginResult result = authService.refreshToken(req);
        return ApiResult.ok(result, "Refresh token success");
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@AuthenticationPrincipal UserPrincipal currentUser) {
        if (currentUser != null) {
            authService.logout(currentUser.getUserId());
        }
        return ApiResult.ok(null, "Logout success");
    }
}
