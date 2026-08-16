package com.app.features.post.videopost.web.view;

import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiConfirmModalView;
import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerVideoDetailPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final SocialShellView shell;
    private final UiBreadcrumbView breadcrumb;
    private final OwnerVideoCardView card;
    private final UiConfirmModalView actionModal;
    private final String openModalId;
}
