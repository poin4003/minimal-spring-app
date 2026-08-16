package com.app.features.post.videopost.web.view;

import java.util.List;

import com.app.features.post.videopost.web.enums.VideoLibraryTab;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicVideoLibraryPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final SocialShellView shell;
    private final VideoLibraryTab activeTab;
    private final String videosPath;
    private final String seriesPath;
    private final String searchPath;
    private final String titleQuery;
    private final List<PublicVideoCardView> videos;
    private final List<VideoSeriesCardView> series;
    private final UiPaginationView pagination;
}
