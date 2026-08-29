package com.app.features.ai.search.schema.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.app.features.post.enums.PostType;

public record PostVectorDocument(
        UUID postId,
        PostType postType,
        LocalDateTime sourceUpdatedAt,
        float[] vector) {

    public PostVectorDocument {
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(postType, "postType must not be null");
        Objects.requireNonNull(
                sourceUpdatedAt,
                "sourceUpdatedAt must not be null");
        Objects.requireNonNull(vector, "vector must not be null");
        if (vector.length == 0) {
            throw new IllegalArgumentException("vector must not be empty");
        }
        vector = vector.clone();
    }

    @Override
    public float[] vector() {
        return vector.clone();
    }
}
