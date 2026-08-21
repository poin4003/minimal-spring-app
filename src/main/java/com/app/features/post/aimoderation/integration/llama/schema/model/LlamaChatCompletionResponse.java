package com.app.features.post.aimoderation.integration.llama.schema.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlamaChatCompletionResponse(
        String model,
        List<LlamaChatChoice> choices) {
}
