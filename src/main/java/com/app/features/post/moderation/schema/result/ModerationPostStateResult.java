package com.app.features.post.moderation.schema.result;

import java.time.LocalDateTime;

import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.user.schema.result.UserShortResult;

import lombok.Data;

@Data
public class ModerationPostStateResult {

    private PostModerationStatus moderationStatus;

    private UserShortResult moderatedBy;

    private LocalDateTime moderatedAt;

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
