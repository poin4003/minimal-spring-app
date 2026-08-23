package com.app.features.post.aimoderation.schema.result;

import java.time.LocalDateTime;

import com.app.features.post.aimoderation.enums.PostAiModerationMode;

import lombok.Data;

@Data
public class PostAiModerationConfigResult {

    private PostAiModerationMode mode;

    private String promptText;

    private LocalDateTime updatedAt;
}
