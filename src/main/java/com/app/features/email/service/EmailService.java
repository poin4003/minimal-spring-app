package com.app.features.email.service;

import com.app.features.email.schema.payload.EmailPayload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface EmailService {

    String send(@NotNull @Valid EmailPayload payload);
}
