package com.app.features.ai.rag.schema.model;

import java.util.Objects;

import com.app.features.ai.rag.enums.PostRagMessageRole;

public record PostRagConversationMessage(
        PostRagMessageRole role,
        String content) {

    public PostRagConversationMessage {
        Objects.requireNonNull(role, "role must not be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "content must not be blank");
        }
        content = content.trim();
    }
}
