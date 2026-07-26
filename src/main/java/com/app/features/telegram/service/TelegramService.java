package com.app.features.telegram.service;

import com.app.features.telegram.schema.payload.TelegramPayload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface TelegramService {

    String send(@NotNull @Valid TelegramPayload payload);
}
