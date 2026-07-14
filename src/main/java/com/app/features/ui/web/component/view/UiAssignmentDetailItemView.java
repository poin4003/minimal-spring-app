package com.app.features.ui.web.component.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiAssignmentDetailItemView {

    private final String title;
    private final String description;
    private final String actionPath;
    private final String actionLabel;
    private final String actionButtonClass;
    private final String hiddenFieldName;
    private final String hiddenFieldValue;
}
