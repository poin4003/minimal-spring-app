package com.app.features.post.videopost.web.view;

import com.app.features.post.videopost.schema.result.VideoSeriesItemResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicVideoSeriesItemView {

    private final VideoSeriesItemResult item;
    private final String detailPath;
    private final boolean active;
}
