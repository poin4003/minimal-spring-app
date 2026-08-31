package com.app.features.ai.rag.schema.model;

import java.util.List;
import java.util.Objects;

import com.app.features.ai.enums.AiAvailability;

public record PostRagStreamMetadata(
        AiAvailability retrievalAvailability,
        AiAvailability generationAvailability,
        List<PostRagSource> sources) {

    public PostRagStreamMetadata {
        Objects.requireNonNull(
                retrievalAvailability,
                "retrievalAvailability must not be null");
        Objects.requireNonNull(
                generationAvailability,
                "generationAvailability must not be null");
        sources = List.copyOf(Objects.requireNonNull(
                sources,
                "sources must not be null"));
    }
}
