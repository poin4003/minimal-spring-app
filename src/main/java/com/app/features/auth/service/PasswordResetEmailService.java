package com.app.features.auth.service;

import java.time.Duration;

import com.app.core.enums.AppLanguage;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface PasswordResetEmailService {

    void sendOtp(
            @NotBlank @Email String recipientEmail,
            @NotBlank String code,
            @NotNull AppLanguage language,
            @NotNull Duration otpTtl);
}
