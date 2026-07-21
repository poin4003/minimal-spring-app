package com.app.features.ui.web.component.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiConfirmModalView {

    private final String id;
    private final String title;
    private final String description;
    private final String actionPath;
    private final String confirmLabel;
    private final String confirmButtonClass;
}
