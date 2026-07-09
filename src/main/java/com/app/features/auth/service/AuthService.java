package com.app.features.auth.service;

import java.util.UUID;

import com.app.features.auth.cqrs.result.LoginResult;

public interface AuthService {
    
    LoginResult generateAndSaveTokens(UUID userId, String userEmail);
}
