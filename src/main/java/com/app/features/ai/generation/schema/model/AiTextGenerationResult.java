package com.app.features.ai.generation.schema.model;

public record AiTextGenerationResult(
        String responseText,
        int promptTokens,
        int generatedTokens,
        long promptTimeMs,
        long generationTimeMs,
        String finishReason) {
}
