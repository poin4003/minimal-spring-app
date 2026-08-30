package com.app.features.ai.rag.schema.model;

import java.util.List;
import java.util.Objects;

public record PostRagPrompt(
        String systemPrompt,
        String userPrompt,
        List<PostRagSource> sources) {

    public PostRagPrompt {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException(
                    "systemPrompt must not be blank");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException(
                    "userPrompt must not be blank");
        }
        systemPrompt = systemPrompt.trim();
        userPrompt = userPrompt.trim();
        sources = List.copyOf(Objects.requireNonNull(
                sources,
                "sources must not be null"));
        if (sources.isEmpty()) {
            throw new IllegalArgumentException(
                    "sources must not be empty");
        }
    }
}
