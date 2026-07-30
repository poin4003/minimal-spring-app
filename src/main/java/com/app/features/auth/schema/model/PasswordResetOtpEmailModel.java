package com.app.features.auth.schema.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PasswordResetOtpEmailModel {

    private final String heading;
    private final String instruction;
    private final String code;
    private final String expiryText;
}
