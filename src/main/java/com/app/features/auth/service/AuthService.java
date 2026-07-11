package com.app.features.auth.service;

import java.util.UUID;

import com.app.features.auth.schema.payload.LoginPayload;
import com.app.features.auth.schema.payload.RefreshTokenPayload;
import com.app.features.auth.schema.result.LoginResult;

public interface AuthService {
    
    LoginResult login(LoginPayload req, String ipAddress);

    LoginResult refreshToken(RefreshTokenPayload req);

    void logout(UUID keyStoreId, UUID userId);
}
