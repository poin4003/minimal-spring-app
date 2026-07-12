package com.app.features.auth.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String applicationTitle;
    private final String loginPath;
    private final boolean hasError;
    private final boolean loggedOut;
}
