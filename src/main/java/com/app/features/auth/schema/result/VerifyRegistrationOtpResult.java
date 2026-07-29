package com.app.features.auth.schema.result;

import java.time.Instant;

import lombok.Data;

@Data
public class VerifyRegistrationOtpResult {

    private String completionToken;
    private Instant expiresAt;
}
