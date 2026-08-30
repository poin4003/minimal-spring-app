package com.app.features.ai.rag.schema.model;

import java.util.List;
import java.util.Objects;

import com.app.features.ai.enums.AiAvailability;

public record PostRagContext(
        String question,
        AiAvailability retrievalAvailability,
        List<PostRagSource> sources) {

    public PostRagContext {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        question = question.trim();
        Objects.requireNonNull(
                retrievalAvailability,
                "retrievalAvailability must not be null");
        sources = List.copyOf(Objects.requireNonNull(
                sources,
                "sources must not be null"));
        if (retrievalAvailability != AiAvailability.READY
                && !sources.isEmpty()) {
            throw new IllegalArgumentException(
                    "unavailable retrieval context must not contain sources");
        }
    }

    public static PostRagContext unavailable(
            String question,
            AiAvailability availability) {
        return new PostRagContext(question, availability, List.of());
    }

    public static PostRagContext ready(
            String question,
            List<PostRagSource> sources) {
        return new PostRagContext(
                question,
                AiAvailability.READY,
                sources);
    }
}
