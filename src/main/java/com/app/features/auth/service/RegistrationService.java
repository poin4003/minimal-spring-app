package com.app.features.auth.service;

import com.app.core.enums.AppLanguage;
import com.app.features.auth.schema.payload.RequestRegistrationOtpPayload;
import com.app.features.auth.schema.result.RequestRegistrationOtpResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface RegistrationService {

    RequestRegistrationOtpResult requestOtp(
            @NotNull @Valid RequestRegistrationOtpPayload payload,
            @NotNull AppLanguage language);
}
