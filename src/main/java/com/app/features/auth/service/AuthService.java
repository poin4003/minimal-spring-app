package com.app.features.auth.service;

import java.util.UUID;

import com.app.features.auth.schema.payload.LoginPayload;
import com.app.features.auth.schema.payload.RefreshTokenPayload;
import com.app.features.auth.schema.result.LoginResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface AuthService {
    
    LoginResult login(
            @NotNull @Valid LoginPayload req,
            String ipAddress);

    LoginResult refreshToken(@NotNull @Valid RefreshTokenPayload req);

    void logout(@NotNull UUID userId, UUID keyStoreId);
}
