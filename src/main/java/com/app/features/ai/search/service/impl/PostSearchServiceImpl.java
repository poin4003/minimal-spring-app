package com.app.features.ai.search.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.search.schema.model.PostSearchCandidate;
import com.app.features.ai.search.schema.model.PostSearchItem;
import com.app.features.ai.search.schema.model.PostSearchRequest;
import com.app.features.ai.search.schema.model.PostSearchResult;
import com.app.features.ai.search.schema.model.PostSemanticSearchResult;
import com.app.features.ai.search.schema.model.PostVectorSearchHit;
import com.app.features.ai.search.service.PostSearchService;
import com.app.features.ai.search.service.PostSemanticSearchService;
import com.app.features.ai.search.support.PostSearchCandidateFactory;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class PostSearchServiceImpl implements PostSearchService {

    private final AppProperties appProperties;
    private final PostSemanticSearchService postSemanticSearchSvc;
    private final PostSearchCandidateFactory postSearchCandidateFactory;

    @Override
    public PostSearchResult search(PostSearchRequest request) {
        String query = request.query().trim();
        int effectiveLimit = Math.min(
                request.limit(),
                appProperties.getAi().getSearch().getMaxLimit());
        int retrievalLimit = request.postType() == null
                ? effectiveLimit
                : appProperties.getAi().getSearch().getMaxLimit();
        PostSemanticSearchResult semanticResult =
                postSemanticSearchSvc.search(query, retrievalLimit);
        if (semanticResult.availability() != AiAvailability.READY) {
            return PostSearchResult.unavailable(
                    query,
                    semanticResult.availability());
        }

        List<PostSearchItem> items = buildItems(
                semanticResult.hits(),
                request,
                effectiveLimit);
        return PostSearchResult.ready(query, items);
    }

    private List<PostSearchItem> buildItems(
            List<PostVectorSearchHit> hits,
            PostSearchRequest request,
            int limit) {
        List<PostSearchItem> items = new ArrayList<>();

        for (PostVectorSearchHit hit : hits) {
            if (request.postType() != null
                    && hit.postType() != request.postType()) {
                continue;
            }

            Optional<PostSearchCandidate> candidateOptional =
                    postSearchCandidateFactory.findIndexable(hit.postId());
            if (candidateOptional.isEmpty()) {
                continue;
            }

            PostSearchCandidate candidate = candidateOptional.get();
            if (candidate.postType() != hit.postType()) {
                continue;
            }

            items.add(new PostSearchItem(
                    items.size() + 1,
                    candidate.postId(),
                    candidate.postType(),
                    hit.score(),
                    candidate.sourceUpdatedAt(),
                    candidate.content()));
            if (items.size() >= limit) {
                break;
            }
        }

        return List.copyOf(items);
    }
}
