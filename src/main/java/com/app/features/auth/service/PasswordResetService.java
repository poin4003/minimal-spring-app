package com.app.features.auth.service;

import com.app.features.auth.schema.payload.RequestPasswordResetOtpPayload;
import com.app.features.auth.schema.result.RequestPasswordResetOtpResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface PasswordResetService {

    RequestPasswordResetOtpResult requestOtp(
            @NotNull @Valid RequestPasswordResetOtpPayload payload);
}
