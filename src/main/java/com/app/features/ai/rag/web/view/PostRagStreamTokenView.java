package com.app.features.ai.rag.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostRagStreamTokenView {

    private final String text;
}
