package com.app.features.post.aimoderation.web.view;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostAiModerationPanelState {

    public static final String ATTRIBUTE = "aiModerationState";

    private final PostAiModerationConfigForm form;

    @Builder.Default
    private final Map<String, String> fieldErrors = Map.of();

    private final boolean saved;
}
