package com.app.features.auth.service;

import com.app.core.enums.AppLanguage;
import com.app.features.auth.schema.payload.CompleteRegistrationPayload;
import com.app.features.auth.schema.payload.RequestRegistrationOtpPayload;
import com.app.features.auth.schema.payload.VerifyRegistrationOtpPayload;
import com.app.features.auth.schema.result.LoginResult;
import com.app.features.auth.schema.result.RequestRegistrationOtpResult;
import com.app.features.auth.schema.result.VerifyRegistrationOtpResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface RegistrationService {

    RequestRegistrationOtpResult requestOtp(
            @NotNull @Valid RequestRegistrationOtpPayload payload,
            @NotNull AppLanguage language);

    VerifyRegistrationOtpResult verifyOtp(
            @NotNull @Valid VerifyRegistrationOtpPayload payload);

    LoginResult completeRegistration(
            @NotNull @Valid CompleteRegistrationPayload payload,
            String ipAddress);
}
