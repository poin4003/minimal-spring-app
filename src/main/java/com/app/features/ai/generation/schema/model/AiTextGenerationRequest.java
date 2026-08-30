package com.app.features.ai.generation.schema.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AiTextGenerationRequest(
        @NotBlank String systemPrompt,
        @NotBlank String userPrompt,
        @DecimalMin("0.0") @DecimalMax("2.0") float temperature,
        @Positive int maxOutputTokens) {
}
