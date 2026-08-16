package com.app.features.post.shortpost.web.view;

import com.app.features.ui.web.view.SocialShellView;
import com.app.features.user.schema.result.ProfileResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerShortListPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final SocialShellView shell;
    private final ProfileResult profile;
    private final String editProfilePath;
    private final String standardPostsPath;
    private final String shortsPath;
    private final String videosPath;
    private final String createPath;
    private final OwnerShortWorkspaceView workspace;
}
