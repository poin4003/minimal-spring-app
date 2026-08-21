package com.app.features.post.aimoderation.schema.model;

import com.app.features.post.aimoderation.enums.PostAiModerationOutcome;

public record PostAiModerationStructuredOutput(
        PostAiModerationOutcome outcome,
        String reason) {
}
