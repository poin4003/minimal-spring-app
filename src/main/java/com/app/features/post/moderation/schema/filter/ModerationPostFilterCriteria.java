package com.app.features.post.moderation.schema.filter;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;

import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.ModerationPostStatusFilter;

import lombok.Data;

@Data
public class ModerationPostFilterCriteria {

    private ModerationPostStatusFilter moderationStatus =
            ModerationPostStatusFilter.PENDING_REVIEW;

    public void setModerationStatus(
            ModerationPostStatusFilter moderationStatus) {
        this.moderationStatus = moderationStatus == null
                ? ModerationPostStatusFilter.ALL
                : moderationStatus;
    }

    private PostType type;

    private UUID authorId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;
}
