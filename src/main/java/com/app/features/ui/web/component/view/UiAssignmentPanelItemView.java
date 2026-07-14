package com.app.features.ui.web.component.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiAssignmentPanelItemView {

    private final String title;
    private final String description;
    private final UiAssignmentActionView action;
}
