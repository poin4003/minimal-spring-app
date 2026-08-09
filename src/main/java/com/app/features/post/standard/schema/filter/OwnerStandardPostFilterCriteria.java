package com.app.features.post.standard.schema.filter;

import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;

import lombok.Data;

@Data
public class OwnerStandardPostFilterCriteria {

    private PostLifecycleStatus lifecycleStatus;

    private PostModerationStatus moderationStatus;
}
