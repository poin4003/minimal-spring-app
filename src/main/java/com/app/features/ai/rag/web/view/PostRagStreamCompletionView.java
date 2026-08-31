package com.app.features.ai.rag.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostRagStreamCompletionView {

    private final boolean generated;
    private final String answer;
}
