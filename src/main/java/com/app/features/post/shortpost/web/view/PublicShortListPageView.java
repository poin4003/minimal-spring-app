package com.app.features.post.shortpost.web.view;

import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicShortListPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final SocialShellView shell;
    private final String createPath;
    private final PublicShortFeedView feed;
}
