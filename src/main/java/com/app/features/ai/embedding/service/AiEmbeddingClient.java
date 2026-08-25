package com.app.features.ai.embedding.service;

import jakarta.validation.constraints.NotBlank;

public interface AiEmbeddingClient {

    float[] embedQuery(@NotBlank String text);

    float[] embedPassage(@NotBlank String text);

    int getDimension();

    String getModelVersion();
}
