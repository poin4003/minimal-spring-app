package com.app.features.post.standard.web.view;

import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicPostListPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final SocialShellView shell;
    private final PublicPostFeedView feed;
}
