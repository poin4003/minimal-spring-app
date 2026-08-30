package com.app.features.ai.rag.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.rag.schema.model.PostRagContext;
import com.app.features.ai.rag.schema.model.PostRagSource;
import com.app.features.ai.search.schema.model.PostSearchCandidate;
import com.app.features.ai.search.schema.model.PostSemanticSearchResult;
import com.app.features.ai.search.schema.model.PostVectorSearchHit;
import com.app.features.ai.search.service.PostSemanticSearchService;
import com.app.features.ai.search.support.PostSearchCandidateFactory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Component
@Validated
@RequiredArgsConstructor
public class PostRagContextFactory {

    private final AppProperties appProperties;
    private final PostSemanticSearchService postSemanticSearchSvc;
    private final PostSearchCandidateFactory postSearchCandidateFactory;

    public PostRagContext create(
            @NotBlank @Size(max = 2000) String question) {
        String normalizedQuestion = question.trim();
        PostSemanticSearchResult searchResult = postSemanticSearchSvc.search(
                normalizedQuestion,
                appProperties.getAi().getRag().getRetrievalLimit());
        if (searchResult.availability() != AiAvailability.READY) {
            return PostRagContext.unavailable(
                    normalizedQuestion,
                    searchResult.availability());
        }

        List<PostRagSource> sources = new ArrayList<>();
        for (PostVectorSearchHit hit : searchResult.hits()) {
            Optional<PostSearchCandidate> candidateOptional =
                    postSearchCandidateFactory.findIndexable(hit.postId());
            if (candidateOptional.isEmpty()) {
                continue;
            }

            PostSearchCandidate candidate = candidateOptional.get();
            if (candidate.postType() != hit.postType()) {
                continue;
            }
            sources.add(new PostRagSource(
                    sources.size() + 1,
                    candidate.postId(),
                    candidate.postType(),
                    hit.score(),
                    candidate.sourceUpdatedAt(),
                    candidate.content()));
        }

        return PostRagContext.ready(normalizedQuestion, sources);
    }
}
