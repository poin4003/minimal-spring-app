package com.app.features.ai.search.web.view;

import java.util.List;

import com.app.features.ai.enums.AiAvailability;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSearchStreamResultsView {

    private final AiAvailability retrievalAvailability;
    private final AiAvailability summaryAvailability;
    private final List<PostSearchItemView> items;
}
