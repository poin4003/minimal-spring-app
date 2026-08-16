package com.app.features.post.videopost.web.view;

import java.util.List;

import com.app.features.post.web.view.OwnerPostStatusFilterView;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.videopost.enums.VideoSeriesLifecycleStatus;
import com.app.features.post.videopost.web.enums.VideoLibraryTab;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerVideoLibraryPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final SocialShellView shell;
    private final VideoLibraryTab activeTab;
    private final String videosPath;
    private final String seriesPath;
    private final String searchPath;
    private final String createVideoPath;
    private final String createSeriesPath;
    private final String titleQuery;
    private final List<OwnerPostStatusFilterView> videoStatusFilters;
    private final List<OwnerPostStatusFilterView> seriesStatusFilters;
    private final PostLifecycleStatus videoLifecycleStatus;
    private final PostModerationStatus videoModerationStatus;
    private final VideoSeriesLifecycleStatus seriesLifecycleStatus;
    private final List<OwnerVideoCardView> videos;
    private final List<OwnerVideoSeriesCardView> series;
    private final UiPaginationView pagination;
}
