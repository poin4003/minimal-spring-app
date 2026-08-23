package com.app.features.post.aimoderation.integration.llama.schema.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LlamaResponseFormat(
        String type,
        JsonNode schema) {
}
