package com.app.features.post.shortpost.web.view;

import com.app.features.post.web.composer.view.PostComposerMediaPickerView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShortPostComposerPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String heading;
    private final String description;
    private final String submitLabel;
    private final String policyHint;
    private final SocialShellView shell;
    private final UiBreadcrumbView breadcrumb;
    private final String actionPath;
    private final String backPath;
    private final String uploadPartialPath;
    private final int maxMediaCount;
    private final PostComposerMediaPickerView mediaPicker;
}
