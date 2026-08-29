package com.app.features.ai.search.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.app.features.ai.search.schema.model.PostSearchIndexClaim;
import com.app.features.ai.search.schema.model.PostSearchIndexWriteResult;

public interface PostSearchIndexStateService {

    void markDirty(UUID postId);

    boolean prepareEnqueue(UUID postId, UUID indexGeneration);

    void markEnqueueFailed(UUID postId);

    Optional<PostSearchIndexClaim> claim(
            UUID postId,
            UUID leaseToken);

    boolean complete(
            PostSearchIndexClaim claim,
            PostSearchIndexWriteResult writeResult);

    boolean fail(PostSearchIndexClaim claim, String errorMessage);

    List<UUID> findRecoveryCandidateIds(
            UUID indexGeneration,
            int limit);

    List<UUID> createBackfillStates(int limit);
}
