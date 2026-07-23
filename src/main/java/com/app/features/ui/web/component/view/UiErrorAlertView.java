package com.app.features.ui.web.component.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiErrorAlertView {

    public static final String ATTRIBUTE = "alert";

    private final String error;
    private final String message;
}
