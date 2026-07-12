package com.app.features.ui.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomePageView {

    public static final String ATTRIBUTE = "page";

    private final UiShellView shell;
}
