package com.app.features.ai.schema.model;

public record JlamaGenerationResult(
        String responseText,
        int promptTokens,
        int generatedTokens,
        long promptTimeMs,
        long generationTimeMs,
        String finishReason) {
}
