package com.app.features.ai.search.web.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.ai.search.schema.model.PostSearchRequest;
import com.app.features.ai.search.schema.model.PostSearchResult;
import com.app.features.ai.search.service.PostSearchService;
import com.app.features.ai.search.service.PostSearchSummaryService;
import com.app.features.ai.search.web.service.PostSearchResultsService;
import com.app.features.ai.search.web.support.PostSearchItemViewFactory;
import com.app.features.ai.search.web.view.PostSearchItemView;
import com.app.features.ai.search.web.view.PostSearchResultsView;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class PostSearchResultsServiceImpl
        implements PostSearchResultsService {

    private final AppProperties appProperties;
    private final PostSearchService postSearchSvc;
    private final PostSearchSummaryService postSearchSummarySvc;
    private final PostSearchItemViewFactory postSearchItemViewFactory;

    @Override
    public PostSearchResultsView search(String query) {
        PostSearchResult result = postSearchSvc.search(
                new PostSearchRequest(
                        query,
                        null,
                        appProperties.getAi().getSearch()
                                .getDefaultLimit()));
        List<PostSearchItemView> items = result.items().stream()
                .map(item -> postSearchItemViewFactory.build(item))
                .toList();

        return PostSearchResultsView.builder()
                .retrievalAvailability(result.availability())
                .summaryAvailability(
                        postSearchSummarySvc.resolveAvailability())
                .items(items)
                .build();
    }
}
