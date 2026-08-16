package com.app.features.post.videopost.web.view;

import java.util.List;

import com.app.features.post.videopost.schema.result.VideoSeriesResult;
import com.app.features.ui.web.component.view.UiPaginationView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicVideoPlaylistView {

    private final VideoSeriesResult series;
    private final List<PublicVideoSeriesItemView> items;
    private final UiPaginationView pagination;
}
