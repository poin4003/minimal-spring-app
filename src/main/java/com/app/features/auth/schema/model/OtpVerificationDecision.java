package com.app.features.auth.schema.model;

import java.time.Instant;

import com.app.features.auth.enums.OtpVerificationStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OtpVerificationDecision {

    private final OtpVerificationStatus status;
    private final Instant completionExpiresAt;
}
