package com.app.features.ai.search.service.impl;

import java.util.concurrent.CancellationException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.generation.schema.model.AiTextGenerationResult;
import com.app.features.ai.generation.service.AiTextGenerationClient;
import com.app.features.ai.generation.service.AiTextGenerationStreamObserver;
import com.app.features.ai.search.schema.model.PostSearchGeneratedSummary;
import com.app.features.ai.search.schema.model.PostSearchSummaryRequest;
import com.app.features.ai.search.schema.model.PostSearchSummaryResult;
import com.app.features.ai.search.service.PostSearchSummaryService;
import com.app.features.ai.search.support.PostSearchSummaryPromptBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class PostSearchSummaryServiceImpl
        implements PostSearchSummaryService {

    private final AppProperties appProperties;
    private final ObjectProvider<AiTextGenerationClient>
            aiTextGenerationClientProvider;
    private final PostSearchSummaryPromptBuilder
            postSearchSummaryPromptBuilder;

    @Override
    public AiAvailability resolveAvailability() {
        if (!appProperties.getAi().getGeneration().isEnabled()) {
            return AiAvailability.DISABLED;
        }

        AiTextGenerationClient generationClient =
                aiTextGenerationClientProvider.getIfAvailable();
        return generationClient != null && generationClient.isReady()
                ? AiAvailability.READY
                : AiAvailability.UNAVAILABLE;
    }

    @Override
    public PostSearchSummaryResult summarize(
            PostSearchSummaryRequest request,
            AiTextGenerationStreamObserver streamObserver) {
        AiAvailability availability = resolveAvailability();
        if (availability != AiAvailability.READY) {
            return PostSearchSummaryResult.unavailable(availability);
        }

        AiTextGenerationClient generationClient =
                aiTextGenerationClientProvider.getIfAvailable();
        if (generationClient == null) {
            return PostSearchSummaryResult.unavailable(
                    AiAvailability.UNAVAILABLE);
        }

        try {
            AiTextGenerationResult generation = generationClient.generate(
                    postSearchSummaryPromptBuilder.build(request),
                    streamObserver);
            if (!StringUtils.hasText(generation.responseText())) {
                log.warn(
                        "Search summary generation returned an empty response.");
                return PostSearchSummaryResult.unavailable(
                        AiAvailability.UNAVAILABLE);
            }

            return PostSearchSummaryResult.ready(
                    new PostSearchGeneratedSummary(
                            generation.responseText(),
                            generationClient.getModelId(),
                            generation.promptTokens(),
                            generation.generatedTokens(),
                            generation.promptTimeMs(),
                            generation.generationTimeMs(),
                            generation.finishReason()));
        } catch (CancellationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn(
                    "Search summary generation failed; returning search "
                            + "results without a generated summary.",
                    exception);
            return PostSearchSummaryResult.unavailable(
                    AiAvailability.UNAVAILABLE);
        }
    }
}
