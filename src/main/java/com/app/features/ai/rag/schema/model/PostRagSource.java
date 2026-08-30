package com.app.features.ai.rag.schema.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.app.features.post.enums.PostType;

public record PostRagSource(
        int rank,
        UUID postId,
        PostType postType,
        float score,
        LocalDateTime sourceUpdatedAt,
        String content) {

    public PostRagSource {
        if (rank <= 0) {
            throw new IllegalArgumentException(
                    "rank must be greater than zero");
        }
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(postType, "postType must not be null");
        if (!Float.isFinite(score)) {
            throw new IllegalArgumentException("score must be finite");
        }
        Objects.requireNonNull(
                sourceUpdatedAt,
                "sourceUpdatedAt must not be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        content = content.trim();
    }
}
