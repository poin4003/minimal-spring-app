package com.app.features.post.shortpost.schema.filter;

import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;

import lombok.Data;

@Data
public class OwnerShortPostFilterCriteria {

    private PostLifecycleStatus lifecycleStatus;

    private PostModerationStatus moderationStatus;
}
