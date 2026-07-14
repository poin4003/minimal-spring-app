package com.app.features.ui.web.component.view;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiAssignmentPanelItemView {

    private final String title;
    private final String description;
    private final String actionPath;
    private final String actionLabel;
    private final String actionButtonClass;
    private final Map<String, String> hiddenFields;
}
