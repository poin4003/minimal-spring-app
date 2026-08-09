package com.app.features.post.standard.web.view;

import com.app.features.ui.web.view.SocialShellView;
import com.app.features.user.schema.result.ProfileResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerPostListPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final SocialShellView shell;
    private final ProfileResult profile;
    private final String editProfilePath;
    private final String shortsPath;
    private final String createPath;
    private final OwnerPostWorkspaceView workspace;
}
