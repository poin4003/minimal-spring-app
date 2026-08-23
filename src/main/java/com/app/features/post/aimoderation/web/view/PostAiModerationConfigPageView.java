package com.app.features.post.aimoderation.web.view;

import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostAiModerationConfigPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;

    private final UiShellView shell;

    private final PostAiModerationPanelView config;
}
