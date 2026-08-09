package com.app.features.post.moderation.schema.filter;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.post.enums.PostType;

import lombok.Data;

@Data
public class ModerationPostFilterCriteria {

    private PostType type;

    private UUID authorId;

    private LocalDateTime createdFrom;

    private LocalDateTime createdTo;
}
