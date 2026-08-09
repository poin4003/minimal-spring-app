package com.app.features.post.shortpost.web.view;

import com.app.features.ui.web.component.view.UiBreadcrumbView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShortPostComposerModeView {

    private final String title;
    private final String description;
    private final String submitLabel;
    private final String actionPath;
    private final String backPath;
    private final UiBreadcrumbView breadcrumb;
}
