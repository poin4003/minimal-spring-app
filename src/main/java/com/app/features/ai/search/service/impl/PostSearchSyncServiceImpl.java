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
import com.app.features.ai.search.schema.model.PostSearchIndexClaim;
import com.app.features.ai.search.schema.model.PostSearchIndexWriteResult;
import com.app.features.ai.search.schema.model.PostVectorDocument;
import com.app.features.ai.search.service.PostSearchIndexStateService;
import com.app.features.ai.search.service.PostSearchQueueService;
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
    private final PostSearchIndexStateService postSearchIndexStateSvc;
    private final PostSearchQueueService postSearchQueueSvc;

    @Override
    public void synchronize(UUID postId) {
        PostVectorIndex postVectorIndex = requirePostVectorIndex();
        Optional<PostSearchIndexClaim> claim = postSearchIndexStateSvc.claim(
                postId,
                UUID.randomUUID());
        if (claim.isEmpty()) {
            return;
        }

        PostSearchIndexClaim claimedState = claim.get();
        PostSearchIndexWriteResult writeResult;
        try {
            writeResult = writeIndex(postId, postVectorIndex);
        } catch (RuntimeException exception) {
            handleWriteFailure(claimedState, exception);
            throw exception;
        }

        if (postSearchIndexStateSvc.complete(claimedState, writeResult)) {
            postSearchQueueSvc.enqueue(postId);
        }
    }

    private PostSearchIndexWriteResult writeIndex(
            UUID postId,
            PostVectorIndex postVectorIndex) {
        Optional<PostSearchCandidate> candidate = postSearchCandidateFactory
                .findIndexable(postId);
        if (candidate.isEmpty()) {
            postVectorIndex.delete(postId);
            log.debug("Removed post [{}] from the search index.", postId);
            return new PostSearchIndexWriteResult(
                    null,
                    postVectorIndex.getModelVersion(),
                    postVectorIndex.getIndexGeneration());
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
        return new PostSearchIndexWriteResult(
                indexablePost.sourceUpdatedAt(),
                postVectorIndex.getModelVersion(),
                postVectorIndex.getIndexGeneration());
    }

    private void handleWriteFailure(
            PostSearchIndexClaim claim,
            RuntimeException writeException) {
        try {
            if (postSearchIndexStateSvc.fail(
                    claim,
                    writeException.getMessage())) {
                postSearchQueueSvc.enqueue(claim.postId());
            }
        } catch (RuntimeException stateException) {
            writeException.addSuppressed(stateException);
            log.error(
                    "Unable to record search synchronization failure for post [{}].",
                    claim.postId(),
                    stateException);
        }
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
