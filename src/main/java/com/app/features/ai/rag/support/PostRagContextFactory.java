package com.app.features.ai.rag.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.rag.schema.model.PostRagConversationMessage;
import com.app.features.ai.rag.schema.model.PostRagConversationRequest;
import com.app.features.ai.rag.schema.model.PostRagContext;
import com.app.features.ai.rag.schema.model.PostRagSource;
import com.app.features.ai.search.schema.model.PostSearchCandidate;
import com.app.features.ai.search.schema.model.PostSemanticSearchResult;
import com.app.features.ai.search.schema.model.PostVectorSearchHit;
import com.app.features.ai.search.service.PostSemanticSearchService;
import com.app.features.ai.search.support.PostSearchCandidateFactory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Component
@Validated
@RequiredArgsConstructor
public class PostRagContextFactory {

    private static final int MAX_RETRIEVAL_QUERY_CHARACTERS = 2000;
    private static final int MAX_RETRIEVAL_HISTORY_MESSAGES = 2;

    private final AppProperties appProperties;
    private final PostSemanticSearchService postSemanticSearchSvc;
    private final PostSearchCandidateFactory postSearchCandidateFactory;

    public PostRagContext create(
            @NotBlank @Size(max = 2000) String question) {
        return create(PostRagConversationRequest.withoutHistory(question));
    }

    public PostRagContext create(
            @NotNull PostRagConversationRequest request) {
        String normalizedQuestion = request.question();
        PostSemanticSearchResult searchResult = postSemanticSearchSvc.search(
                buildRetrievalQuery(request),
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

    private String buildRetrievalQuery(
            PostRagConversationRequest request) {
        StringBuilder query = new StringBuilder(request.question());
        int includedMessages = 0;

        for (int index = request.history().size() - 1;
                index >= 0
                        && includedMessages
                                < MAX_RETRIEVAL_HISTORY_MESSAGES;
                index--) {
            PostRagConversationMessage message = request.history().get(index);
            query.insert(
                    0,
                    message.role().name()
                            + ": "
                            + message.content()
                            + "\n");
            includedMessages++;
        }

        if (query.length() <= MAX_RETRIEVAL_QUERY_CHARACTERS) {
            return query.toString();
        }
        return query.substring(
                query.length() - MAX_RETRIEVAL_QUERY_CHARACTERS);
    }
}
