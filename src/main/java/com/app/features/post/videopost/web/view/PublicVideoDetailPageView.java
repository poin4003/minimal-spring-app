package com.app.features.post.videopost.web.view;

import com.app.features.post.videopost.schema.result.PublicVideoPostResult;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicVideoDetailPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final SocialShellView shell;
    private final UiBreadcrumbView breadcrumb;
    private final PublicVideoPostResult video;
    private final PublicVideoPlaylistView playlist;
}
