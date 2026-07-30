package com.app.features.auth.schema.result;

import java.time.Instant;

import lombok.Data;

@Data
public class VerifyPasswordResetOtpResult {

    private String resetToken;
    private Instant expiresAt;
}
