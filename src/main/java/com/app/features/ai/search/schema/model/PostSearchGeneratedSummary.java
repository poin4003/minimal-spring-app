package com.app.features.ai.search.schema.model;

public record PostSearchGeneratedSummary(
        String text,
        String modelId,
        int promptTokens,
        int generatedTokens,
        long promptTimeMs,
        long generationTimeMs,
        String finishReason) {

    public PostSearchGeneratedSummary {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("modelId must not be blank");
        }
        if (promptTokens < 0 || generatedTokens < 0) {
            throw new IllegalArgumentException(
                    "token counts must not be negative");
        }
        if (promptTimeMs < 0 || generationTimeMs < 0) {
            throw new IllegalArgumentException(
                    "generation durations must not be negative");
        }
        if (finishReason == null || finishReason.isBlank()) {
            throw new IllegalArgumentException(
                    "finishReason must not be blank");
        }
        text = text.trim();
        modelId = modelId.trim();
        finishReason = finishReason.trim();
    }
}
