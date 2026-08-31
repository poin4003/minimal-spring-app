package com.app.features.ai.rag.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostRagChatSourceView {

    private final int rank;
    private final String postTypeLabel;
    private final int relevancePercent;
    private final String content;
    private final String detailPath;
}
