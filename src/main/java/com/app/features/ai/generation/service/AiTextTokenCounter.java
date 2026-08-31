package com.app.features.ai.generation.service;

import jakarta.validation.constraints.NotBlank;

public interface AiTextTokenCounter {

    boolean isReady();

    int countTokens(@NotBlank String text);
}
