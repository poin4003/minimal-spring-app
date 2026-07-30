package com.app.features.auth.service;

import com.app.features.auth.schema.payload.RequestPasswordResetOtpPayload;
import com.app.features.auth.schema.payload.CompletePasswordResetPayload;
import com.app.features.auth.schema.payload.VerifyPasswordResetOtpPayload;
import com.app.features.auth.schema.result.RequestPasswordResetOtpResult;
import com.app.features.auth.schema.result.VerifyPasswordResetOtpResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface PasswordResetService {

    RequestPasswordResetOtpResult requestOtp(
            @NotNull @Valid RequestPasswordResetOtpPayload payload);

    VerifyPasswordResetOtpResult verifyOtp(
            @NotNull @Valid VerifyPasswordResetOtpPayload payload);

    void completePasswordReset(
            @NotNull @Valid CompletePasswordResetPayload payload);
}
