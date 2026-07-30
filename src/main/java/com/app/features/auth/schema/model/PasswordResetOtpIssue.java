package com.app.features.auth.schema.model;

import java.time.Instant;
import java.util.UUID;

import com.app.core.enums.AppLanguage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PasswordResetOtpIssue {

    private final UUID passwordResetId;
    private final String email;
    private final AppLanguage language;
    private final Instant expiresAt;
    private final Instant resendAvailableAt;
}
