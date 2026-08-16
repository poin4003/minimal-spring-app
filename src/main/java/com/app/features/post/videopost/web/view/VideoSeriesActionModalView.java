package com.app.features.post.videopost.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoSeriesActionModalView {

    public static final String ATTRIBUTE = "modal";

    private final String id;
    private final String title;
    private final String description;
    private final String actionPath;
    private final String submitLabel;
    private final String submitButtonClass;
}
