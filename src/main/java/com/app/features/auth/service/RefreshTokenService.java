package com.app.features.auth.service;

public interface RefreshTokenService {
    
    void cleanupExpiredConsumedTokens();
}
