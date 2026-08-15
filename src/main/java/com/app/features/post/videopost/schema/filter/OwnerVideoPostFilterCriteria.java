package com.app.features.post.videopost.schema.filter;

import java.util.UUID;

import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;

import lombok.Data;

@Data
public class OwnerVideoPostFilterCriteria {

    private PostLifecycleStatus lifecycleStatus;

    private PostModerationStatus moderationStatus;

    private String title;

    private UUID seriesId;
}
