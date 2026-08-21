package com.app.features.post.aimoderation.schema.model;

import com.app.features.post.aimoderation.enums.PostAiModerationOutcome;

public record PostAiModerationClientResult(
        PostAiModerationOutcome outcome,
        String reason,
        String rawResponse,
        String modelName) {
}
