package com.app.features.post.aimoderation.schema.model;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record PostAiModerationRequest(
        @NotBlank String systemPrompt,
        @NotBlank String userPrompt,
        List<String> imageUrls) {

    public PostAiModerationRequest {
        imageUrls = imageUrls == null
                ? List.of()
                : List.copyOf(imageUrls);
    }
}
