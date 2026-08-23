package com.app.features.post.aimoderation.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostAiModerationDecisionLogModalView {

    public static final String ATTRIBUTE = "decisionLogModal";

    private final String id;

    private final String title;

    private final String postId;

    private final String outcome;

    private final String modelName;

    private final String createdAt;

    private final String reason;

    private final String errorMessage;

    private final String promptSnapshot;

    private final String rawResponse;
}
