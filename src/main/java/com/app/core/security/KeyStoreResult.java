package com.app.core.security;

import java.util.UUID;

import lombok.Data;

@Data
public class KeyStoreResult {

    private UUID keyStoreId;

    private UUID userId;

    private String publicKey;

    private String privateKey;

    private String refreshToken;
}
