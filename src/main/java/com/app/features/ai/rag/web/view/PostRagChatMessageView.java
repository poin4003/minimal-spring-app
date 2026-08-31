package com.app.features.ai.rag.web.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostRagChatMessageView {

    public static final String ATTRIBUTE = "message";

    private final boolean generated;
    private final String answer;
    private final List<PostRagChatSourceView> sources;
}
