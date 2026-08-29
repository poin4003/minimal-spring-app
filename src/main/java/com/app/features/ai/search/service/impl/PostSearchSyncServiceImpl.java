package com.app.features.ai.search.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.features.ai.embedding.service.AiEmbeddingClient;
import com.app.features.ai.search.exceptions.AiSearchRuntimeException;
import com.app.features.ai.search.schema.model.PostSearchCandidate;
import com.app.features.ai.search.schema.model.PostVectorDocument;
import com.app.features.ai.search.service.PostSearchSyncService;
import com.app.features.ai.search.service.PostVectorIndex;
import com.app.features.ai.search.support.PostSearchCandidateFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.ai.search",
        name = "enabled",
        havingValue = "true")
public class PostSearchSyncServiceImpl implements PostSearchSyncService {

    private final PostSearchCandidateFactory postSearchCandidateFactory;
    private final ObjectProvider<AiEmbeddingClient>
            aiEmbeddingClientProvider;
    private final ObjectProvider<PostVectorIndex> postVectorIndexProvider;

    @Override
    public void synchronize(UUID postId) {
        PostVectorIndex postVectorIndex = requirePostVectorIndex();
        Optional<PostSearchCandidate> candidate = postSearchCandidateFactory
                .findIndexable(postId);
        if (candidate.isEmpty()) {
            postVectorIndex.delete(postId);
            log.debug("Removed post [{}] from the search index.", postId);
            return;
        }

        PostSearchCandidate indexablePost = candidate.get();
        float[] vector = requireEmbeddingClient().embedPassage(
                indexablePost.content());
        postVectorIndex.upsert(new PostVectorDocument(
                indexablePost.postId(),
                indexablePost.postType(),
                indexablePost.sourceUpdatedAt(),
                vector));
        log.debug("Synchronized post [{}] to the search index.", postId);
    }

    private AiEmbeddingClient requireEmbeddingClient() {
        AiEmbeddingClient embeddingClient =
                aiEmbeddingClientProvider.getIfAvailable();
        if (embeddingClient == null) {
            throw new AiSearchRuntimeException(
                    "AI embedding runtime is unavailable for search synchronization.");
        }
        return embeddingClient;
    }

    private PostVectorIndex requirePostVectorIndex() {
        PostVectorIndex postVectorIndex =
                postVectorIndexProvider.getIfAvailable();
        if (postVectorIndex == null) {
            throw new AiSearchRuntimeException(
                    "Post vector index is unavailable for synchronization.");
        }
        return postVectorIndex;
    }
}
