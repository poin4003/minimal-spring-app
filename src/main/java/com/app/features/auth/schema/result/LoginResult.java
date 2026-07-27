package com.app.features.auth.schema.result;

import java.util.UUID;

import lombok.Data;

@Data
public class LoginResult {

    private UUID userId;

    private String accessToken;

    private String refreshToken;
}
