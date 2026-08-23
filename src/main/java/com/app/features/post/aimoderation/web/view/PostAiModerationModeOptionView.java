package com.app.features.post.aimoderation.web.view;

import com.app.features.post.aimoderation.enums.PostAiModerationMode;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostAiModerationModeOptionView {

    private final PostAiModerationMode value;

    private final String label;

    private final boolean disabled;
}
