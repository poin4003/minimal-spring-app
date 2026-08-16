package com.app.features.post.videopost.web.view;

import java.util.List;

import com.app.features.post.videopost.schema.result.VideoSeriesResult;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicVideoSeriesDetailPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final SocialShellView shell;
    private final UiBreadcrumbView breadcrumb;
    private final VideoSeriesResult series;
    private final List<PublicVideoSeriesItemView> items;
    private final UiPaginationView pagination;
    private final VideoSeriesItemSortView itemSort;
}
