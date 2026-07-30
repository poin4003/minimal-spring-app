package com.app.features.auth.schema.result;

import java.time.Instant;

import lombok.Data;

@Data
public class RequestPasswordResetOtpResult {

    private Instant expiresAt;
    private Instant resendAvailableAt;
}
