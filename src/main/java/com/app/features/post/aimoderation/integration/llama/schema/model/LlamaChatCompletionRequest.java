package com.app.features.post.aimoderation.integration.llama.schema.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LlamaChatCompletionRequest(
        String model,
        List<LlamaChatMessage> messages,
        double temperature,
        @JsonProperty("max_tokens") Integer maxTokens,
        @JsonProperty("response_format") LlamaResponseFormat responseFormat) {
}
