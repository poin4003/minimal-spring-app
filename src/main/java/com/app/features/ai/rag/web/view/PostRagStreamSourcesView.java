package com.app.features.ai.rag.web.view;

import java.util.List;

import com.app.features.ai.enums.AiAvailability;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostRagStreamSourcesView {

    private final AiAvailability retrievalAvailability;
    private final AiAvailability generationAvailability;
    private final List<PostRagChatSourceView> sources;
}
