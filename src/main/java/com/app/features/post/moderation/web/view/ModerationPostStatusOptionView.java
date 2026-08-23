package com.app.features.post.moderation.web.view;

import com.app.features.post.moderation.enums.ModerationPostStatusFilter;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModerationPostStatusOptionView {

    private final ModerationPostStatusFilter value;

    private final String label;
}
