package com.app.features.ai.search.schema.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.app.features.post.enums.PostType;

public record PostSearchCandidate(
        UUID postId,
        PostType postType,
        LocalDateTime sourceUpdatedAt,
        String content) {

    public PostSearchCandidate {
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(postType, "postType must not be null");
        Objects.requireNonNull(
                sourceUpdatedAt,
                "sourceUpdatedAt must not be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        content = content.trim();
    }
}
