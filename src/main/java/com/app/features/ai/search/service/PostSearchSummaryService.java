package com.app.features.ai.search.service;

import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.generation.service.AiTextGenerationStreamObserver;
import com.app.features.ai.search.schema.model.PostSearchSummaryRequest;
import com.app.features.ai.search.schema.model.PostSearchSummaryResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface PostSearchSummaryService {

    AiAvailability resolveAvailability();

    PostSearchSummaryResult summarize(
            @NotNull @Valid PostSearchSummaryRequest request,
            @NotNull AiTextGenerationStreamObserver streamObserver);
}
