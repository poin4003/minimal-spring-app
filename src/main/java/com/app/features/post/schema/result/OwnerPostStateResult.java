package com.app.features.post.schema.result;

import java.time.LocalDateTime;

import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;

import lombok.Data;

@Data
public class OwnerPostStateResult {

    private PostLifecycleStatus lifecycleStatus;

    private PostModerationStatus moderationStatus;

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
