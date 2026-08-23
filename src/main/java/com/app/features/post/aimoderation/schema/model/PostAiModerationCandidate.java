package com.app.features.post.aimoderation.schema.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostAiModerationCandidate(
        UUID postId,
        LocalDateTime postUpdatedAt,
        LocalDateTime configUpdatedAt,
        String promptSnapshot,
        PostAiModerationRequest request) {
}
