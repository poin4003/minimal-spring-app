package com.app.features.post.aimoderation.schema.result;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.post.aimoderation.enums.PostAiModerationOutcome;

import lombok.Data;

@Data
public class PostAiModerationDecisionLogResult {

    private UUID id;

    private UUID postId;

    private PostAiModerationOutcome outcome;

    private String reason;

    private String errorMessage;

    private String modelName;

    private LocalDateTime createdAt;
}
