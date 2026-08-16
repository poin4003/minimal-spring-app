package com.app.features.post.videopost.web.view;

import java.util.List;

import com.app.features.post.videopost.schema.result.VideoSeriesItemResult;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerVideoSeriesDetailPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final SocialShellView shell;
    private final UiBreadcrumbView breadcrumb;
    private final OwnerVideoSeriesCardView card;
    private final List<VideoSeriesItemResult> items;
    private final UiPaginationView pagination;
    private final String addItemsPath;
    private final String createVideoPath;
    private final String removeItemPathPrefix;
    private final List<VideoSeriesActionView> actions;
    private final VideoSeriesActionModalView actionModal;
    private final String openModalId;
}
