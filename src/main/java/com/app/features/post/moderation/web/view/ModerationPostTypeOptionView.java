package com.app.features.post.moderation.web.view;

import com.app.features.post.enums.PostType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModerationPostTypeOptionView {

    private final PostType value;
    private final String label;
}
