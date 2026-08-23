package com.app.features.post.aimoderation.web.view;

import java.util.UUID;

import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostAiModerationLogPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;

    private final UiShellView shell;

    private final UiBreadcrumbView breadcrumb;

    private final UUID postId;

    private final String moderationPath;

    private final UiTableView logs;
}
