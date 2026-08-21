package com.app.features.post.aimoderation.integration.llama.schema.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlamaChatChoice(
        int index,
        LlamaAssistantMessage message) {
}
