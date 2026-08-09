package com.app.features.user.web.view;

import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.ui.web.component.view.UiAssignmentPanelView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileAvatarPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String listPath;
    private final String backPath;
    private final String uploadPartialPath;
    private final SocialShellView shell;
    private final UiBreadcrumbView breadcrumb;
    private final MediaFilterCriteria filter;
    private final UiAssignmentPanelView assignmentPanel;
}
