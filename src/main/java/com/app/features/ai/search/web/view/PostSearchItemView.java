package com.app.features.ai.search.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSearchItemView {

    private final int rank;
    private final String postTypeLabel;
    private final String content;
    private final String detailPath;
}
