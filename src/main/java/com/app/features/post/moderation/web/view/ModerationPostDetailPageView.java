package com.app.features.post.moderation.web.view;

import com.app.features.post.moderation.schema.result.ModerationStandardPostDetailResult;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiConfirmModalView;
import com.app.features.ui.web.component.view.UiModalView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModerationPostDetailPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final UiShellView shell;
    private final UiBreadcrumbView breadcrumb;
    private final ModerationStandardPostDetailResult post;
    private final String statusLabel;
    private final String queuePath;
    private final String refreshEvent;
    private final String publishModalPath;
    private final String rejectModalPath;
    private final UiConfirmModalView publishModal;
    private final UiModalView rejectModal;
    private final String openModalId;
}
