package com.app.features.ai.rag.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.core.enums.AppLanguage;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.generation.service.impl.NoOpAiTextGenerationStreamObserver;
import com.app.features.ai.rag.schema.model.PostRagGeneratedAnswer;
import com.app.features.ai.rag.schema.model.PostRagResult;
import com.app.features.ai.rag.schema.model.PostRagSource;
import com.app.features.ai.rag.service.PostRagService;
import com.app.features.ai.search.schema.model.PostSearchGeneratedSummary;
import com.app.features.ai.search.schema.model.PostSearchItem;
import com.app.features.ai.search.schema.model.PostSearchRequest;
import com.app.features.ai.search.schema.model.PostSearchResult;
import com.app.features.ai.search.schema.model.PostSearchSummaryRequest;
import com.app.features.ai.search.schema.model.PostSearchSummaryResult;
import com.app.features.ai.search.service.PostSearchService;
import com.app.features.ai.search.service.PostSearchSummaryService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class PostRagServiceImpl implements PostRagService {

    private final AppProperties appProperties;
    private final PostSearchService postSearchSvc;
    private final PostSearchSummaryService postSearchSummarySvc;

    @Override
    public PostRagResult answer(String question) {
        return answer(question, AppLanguage.EN);
    }

    @Override
    public PostRagResult answer(
            String question,
            AppLanguage responseLanguage) {
        PostSearchResult searchResult = postSearchSvc.search(
                new PostSearchRequest(
                        question,
                        null,
                        appProperties.getAi().getRag()
                                .getRetrievalLimit()));
        List<PostRagSource> sources = toSources(searchResult.items());
        AiAvailability generationAvailability =
                postSearchSummarySvc.resolveAvailability();

        if (searchResult.availability() != AiAvailability.READY
                || searchResult.items().isEmpty()
                || generationAvailability != AiAvailability.READY) {
            return withoutSummary(
                    searchResult,
                    generationAvailability,
                    sources);
        }

        PostSearchSummaryResult summaryResult =
                postSearchSummarySvc.summarize(
                        new PostSearchSummaryRequest(
                                searchResult,
                                responseLanguage),
                        NoOpAiTextGenerationStreamObserver.INSTANCE);
        if (!summaryResult.isGenerated()) {
            return withoutSummary(
                    searchResult,
                    summaryResult.availability(),
                    sources);
        }

        PostSearchGeneratedSummary summary =
                summaryResult.generatedSummary();
        PostRagGeneratedAnswer generatedAnswer =
                new PostRagGeneratedAnswer(
                        summary.text(),
                        summary.modelId(),
                        summary.promptTokens(),
                        summary.generatedTokens(),
                        summary.promptTimeMs(),
                        summary.generationTimeMs(),
                        summary.finishReason());
        return new PostRagResult(
                searchResult.query(),
                searchResult.availability(),
                summaryResult.availability(),
                sources,
                generatedAnswer);
    }

    private List<PostRagSource> toSources(
            List<PostSearchItem> items) {
        return items.stream()
                .map(item -> new PostRagSource(
                        item.rank(),
                        item.postId(),
                        item.postType(),
                        item.score(),
                        item.sourceUpdatedAt(),
                        item.content()))
                .toList();
    }

    private PostRagResult withoutSummary(
            PostSearchResult searchResult,
            AiAvailability generationAvailability,
            List<PostRagSource> sources) {
        return new PostRagResult(
                searchResult.query(),
                searchResult.availability(),
                generationAvailability,
                sources,
                null);
    }
}
