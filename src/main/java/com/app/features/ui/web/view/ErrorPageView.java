package com.app.features.ui.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorPageView {

    public static final String ATTRIBUTE = "page";

    private final int status;
    private final String title;
    private final String error;
    private final String message;
    private final String path;
}
