package com.app.features.auth.api.v1.controller;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.core.response.ApiResult;
import com.app.core.security.UserPrincipal;
import com.app.features.auth.api.v1.dto.request.LoginDto;
import com.app.features.auth.api.v1.dto.request.RefreshTokenDto;
import com.app.features.auth.api.v1.dto.response.LoginResponseDto;
import com.app.features.auth.cqrs.command.LoginCmd;
import com.app.features.auth.cqrs.command.LogoutCmd;
import com.app.features.auth.cqrs.command.RefreshTokenCmd;
import com.app.features.auth.cqrs.result.LoginResult;

import an.awesome.pipelinr.Pipeline;
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

    private final Pipeline pipeline;
    private final ModelMapper mapper;

    @PostMapping("/login")
    public ApiResult<LoginResponseDto> login(
            @Valid @RequestBody LoginDto req
    // @ClientIp String ipAddress
    ) {
        String ipAddress = "192.168.1.100";

        LoginCmd cmd = new LoginCmd(
                req.getEmail(),
                req.getPassword(),
                ipAddress);

        LoginResult result = pipeline.send(cmd);

        return ApiResult.ok(mapper.map(result, LoginResponseDto.class), "Login success");
    }

    @PostMapping("/refresh")
    public ApiResult<LoginResponseDto> refreshToken(
            @RequestBody RefreshTokenDto req) {
        RefreshTokenCmd cmd = new RefreshTokenCmd(req.getRefreshToken());

        LoginResult result = pipeline.send(cmd);

        return ApiResult.ok(mapper.map(result, LoginResponseDto.class), "Refresh token success");
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@AuthenticationPrincipal UserPrincipal currentUser) {
        if (currentUser != null && currentUser.getKeyStore() != null) {
            LogoutCmd cmd = new LogoutCmd(
                    currentUser.getKeyStore().getKeyStoreId(),
                    currentUser.getUserId());

            pipeline.send(cmd);
        }
        return ApiResult.ok(null, "Logout success");
    }
}
