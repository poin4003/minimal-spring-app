package com.app.features.ai.search.schema.model;

import java.util.UUID;

import com.app.features.post.enums.PostType;

public record PostVectorSearchHit(
        UUID postId,
        PostType postType,
        float score) {
}
