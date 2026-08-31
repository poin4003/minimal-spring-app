package com.app.features.ai.search.schema.model;

import com.app.features.post.enums.PostType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PostSearchRequest(
        @NotBlank @Size(max = 2000) String query,
        PostType postType,
        @Positive @Max(100) int limit) {
}
