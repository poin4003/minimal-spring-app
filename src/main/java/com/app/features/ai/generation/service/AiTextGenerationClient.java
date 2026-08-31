package com.app.features.ai.generation.service;

import com.app.features.ai.generation.schema.model.AiTextGenerationRequest;
import com.app.features.ai.generation.schema.model.AiTextGenerationResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface AiTextGenerationClient {

    boolean isReady();

    String getModelId();

    AiTextGenerationResult generate(
            @NotNull @Valid AiTextGenerationRequest request);

    AiTextGenerationResult generate(
            @NotNull @Valid AiTextGenerationRequest request,
            @NotNull AiTextGenerationStreamObserver streamObserver);
}
