package com.app.features.post.aimoderation.integration.llama.schema.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LlamaChatContentItem(
        String type,
        String text,
        @JsonProperty("image_url") LlamaImageUrl imageUrl) {

    public static LlamaChatContentItem text(String text) {
        return new LlamaChatContentItem("text", text, null);
    }

    public static LlamaChatContentItem image(String url) {
        return new LlamaChatContentItem("image_url", null, new LlamaImageUrl(url));
    }
}
