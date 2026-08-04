package com.app.features.post.moderation.schema.result;

import java.util.UUID;

import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.user.schema.result.UserPublicResult;

import lombok.Data;

@Data
public class ModerationPostResult {

    private UUID id;

    private PostType type;

    private UserPublicResult author;

    private PostModerationStatus moderationStatus;

    private String createdAt;
}
