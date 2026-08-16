package com.app.features.post.videopost.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoSeriesItemSortView {

    private final boolean ascending;
    private final String togglePath;
}
