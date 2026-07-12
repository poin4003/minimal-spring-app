package com.app.core.security;

import java.util.UUID;

import lombok.Data;

@Data
public class KeyStoreResult {

    private UUID keyStoreId;

    private UUID userId;

    private String signingKey;

    private String refreshToken;
}
