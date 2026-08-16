package com.app.features.post.videopost.web.view;

import com.app.features.post.videopost.schema.result.VideoSeriesResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerVideoSeriesCardView {

    private final VideoSeriesResult series;
    private final String detailPath;
    private final String editPath;
    private final String statusLabel;
    private final String statusBadgeClass;
}
