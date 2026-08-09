package com.app.features.post.moderation.schema.filter;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;

import com.app.features.post.enums.PostType;

import lombok.Data;

@Data
public class ModerationPostFilterCriteria {

    private PostType type;

    private UUID authorId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;
}
