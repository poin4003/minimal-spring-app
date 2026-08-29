package com.app.features.ai.search.schema.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record PostSearchIndexWriteResult(
        LocalDateTime indexedSourceUpdatedAt,
        String modelVersion,
        UUID indexGeneration) {

    public PostSearchIndexWriteResult {
        if (modelVersion == null || modelVersion.isBlank()) {
            throw new IllegalArgumentException("modelVersion must not be blank");
        }
        Objects.requireNonNull(
                indexGeneration,
                "indexGeneration must not be null");
    }
}
