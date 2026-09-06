package com.app.features.user.web.view;

import com.app.features.user.schema.result.UserPublicResult;
import com.app.features.user.web.enums.PublicProfileContentType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicProfileHeaderView {

    private final UserPublicResult profile;
    private final PublicProfileContentType activeContentType;
    private final String postsPath;
    private final String shortsPath;
    private final String videosPath;
}
