package com.app.features.ai.search.web.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSearchItemView {

    private final String content;
    private final String detailPath;
    private final int relevancePercent;
    private final List<PostSearchMediaView> media;
}
