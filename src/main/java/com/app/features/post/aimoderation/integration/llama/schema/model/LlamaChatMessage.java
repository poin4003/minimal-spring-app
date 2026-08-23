package com.app.features.post.aimoderation.integration.llama.schema.model;

import java.util.List;

public record LlamaChatMessage(
        String role,
        List<LlamaChatContentItem> content) {
}
