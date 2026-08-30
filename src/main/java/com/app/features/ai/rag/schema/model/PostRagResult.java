package com.app.features.ai.rag.schema.model;

import java.util.List;
import java.util.Objects;

import com.app.features.ai.enums.AiAvailability;

public record PostRagResult(
        String question,
        AiAvailability retrievalAvailability,
        AiAvailability generationAvailability,
        List<PostRagSource> sources,
        PostRagGeneratedAnswer generatedAnswer) {

    public PostRagResult {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        question = question.trim();
        Objects.requireNonNull(
                retrievalAvailability,
                "retrievalAvailability must not be null");
        Objects.requireNonNull(
                generationAvailability,
                "generationAvailability must not be null");
        sources = List.copyOf(Objects.requireNonNull(
                sources,
                "sources must not be null"));

        if (retrievalAvailability != AiAvailability.READY
                && !sources.isEmpty()) {
            throw new IllegalArgumentException(
                    "unavailable retrieval must not contain sources");
        }
        if (generatedAnswer != null
                && (retrievalAvailability != AiAvailability.READY
                        || generationAvailability != AiAvailability.READY
                        || sources.isEmpty())) {
            throw new IllegalArgumentException(
                    "generated answer requires ready retrieval, generation, and sources");
        }
    }

    public boolean hasGeneratedAnswer() {
        return generatedAnswer != null;
    }
}
