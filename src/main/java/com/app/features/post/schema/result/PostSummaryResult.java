package com.app.features.post.schema.result;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.post.enums.PostType;
import com.app.features.user.schema.result.UserPublicResult;

import lombok.Data;

@Data
public class PostSummaryResult {

    private UUID id;

    private PostType type;

    private UserPublicResult author;

    private LocalDateTime publishedAt;
}
