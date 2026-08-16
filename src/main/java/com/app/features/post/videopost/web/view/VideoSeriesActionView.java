package com.app.features.post.videopost.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoSeriesActionView {

    private final String label;
    private final String iconClass;
    private final String buttonClass;
    private final String modalPath;
}
