package com.app.features.ui.web.component.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiModalView {

    public static final String ATTRIBUTE = "modal";

    private final String id;
    private final String title;
    private final String description;
    private final String triggerLabel;
    private final String triggerButtonClass;
    private final String dialogClass;
    private final String actionPath;
    private final String method;
    private final String submitLabel;
    private final List<UiModalFieldView> fields;
}
