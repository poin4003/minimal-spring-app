package com.app.features.ai.search.web.view;

import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSearchPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String searchPath;
    private final String resultsPath;
    private final String summaryStreamPath;
    private final String query;
    private final SocialShellView shell;
}
