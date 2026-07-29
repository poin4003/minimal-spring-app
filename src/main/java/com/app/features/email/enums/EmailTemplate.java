package com.app.features.email.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailTemplate {
    REGISTRATION_OTP("email/registration-otp");

    private final String path;
}
