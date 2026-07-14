package com.app.features.user.web.view;

import java.util.List;

import com.app.features.ui.web.component.view.UiAssignmentPanelView;
import com.app.features.ui.web.component.view.UiMetadataItemView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRolePageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String heading;
    private final String description;
    private final List<UiMetadataItemView> metadataItems;
    private final UiShellView shell;
    private final String backPath;
    private final String assignedPath;
    private final String availablePath;
    private final boolean assignedMode;
    private final UiAssignmentPanelView assignmentPanel;
    private final String errorMessage;
}
