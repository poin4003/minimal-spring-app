package com.app.features.ui.web.component.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiModalDefinition {

    private final String id;
    private final String title;
    private final String description;
    private final String triggerLabel;
    private final String triggerButtonClass;
    private final String dialogClass;
    private final String actionPath;

    @Builder.Default
    private final String method = "post";

    @Builder.Default
    private final String submitLabel = "Save changes";
}
