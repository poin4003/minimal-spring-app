package com.app.features.post.moderation.schema.result;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.user.schema.result.UserPublicResult;
import com.app.features.user.schema.result.UserShortResult;

import lombok.Data;

@Data
public class ModerationStandardPostDetailResult {
    
    private UUID id;

    private PostType type;

    private UserPublicResult author;

    private String content;

    private List<ModerationPostMediaResult> media = List.of();

    private PostModerationStatus moderationStatus;

    private UserShortResult moderatorBy;

    private LocalDateTime moderatedAt;

    private LocalDateTime publishedAt;

    private String rejectedReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
