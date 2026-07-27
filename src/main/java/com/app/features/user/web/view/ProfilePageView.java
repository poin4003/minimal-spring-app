package com.app.features.user.web.view;

import com.app.features.ui.web.view.UiShellView;
import com.app.features.user.schema.result.ProfileResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfilePageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String updatePath;
    private final String avatarSelectionPath;
    private final String removeAvatarPath;
    private final UiShellView shell;
    private final ProfileResult profile;
}
