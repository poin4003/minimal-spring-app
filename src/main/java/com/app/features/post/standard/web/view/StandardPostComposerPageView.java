package com.app.features.post.standard.web.view;

import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StandardPostComposerPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final SocialShellView shell;
    private final UiBreadcrumbView breadcrumb;
    private final String actionPath;
    private final String backPath;
    private final String uploadPartialPath;
    private final int maxMediaCount;
    private final PostComposerMediaPickerView mediaPicker;
}
