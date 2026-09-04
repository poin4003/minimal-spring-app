package com.app.features.ai.search.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.app.features.ai.search.entity.PostSearchIndexStateEntity;
import com.app.features.ai.search.enums.PostSearchIndexStatus;
import com.app.features.ai.search.repository.PostSearchIndexStateRepository;
import com.app.features.ai.search.schema.model.PostSearchIndexClaim;
import com.app.features.ai.search.schema.model.PostSearchIndexWriteResult;
import com.app.features.ai.search.service.PostSearchIndexStateService;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.PostModerationStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostSearchIndexStateServiceImpl
        implements PostSearchIndexStateService {

    private static final Duration QUEUE_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration RETRY_DELAY = Duration.ofMinutes(5);
    private static final int MAX_ERROR_LENGTH = 2000;
    private static final Set<PostType> SEARCHABLE_POST_TYPES = Set.of(
            PostType.STANDARD,
            PostType.SHORT,
            PostType.VIDEO);
    private static final Set<PostSearchIndexStatus> LEASED_STATUSES = Set.of(
            PostSearchIndexStatus.QUEUED,
            PostSearchIndexStatus.PROCESSING);

    private final PostSearchIndexStateRepository postSearchIndexStateRepo;

    @Override
    @Transactional
    public void markDirty(UUID postId) {
        LocalDateTime now = LocalDateTime.now();
        Optional<PostSearchIndexStateEntity> existingState =
                postSearchIndexStateRepo.findForUpdateByPostId(postId);
        PostSearchIndexStateEntity state = existingState
                .orElseGet(() -> newState(postId));
        if (existingState.isPresent()) {
            state.setRequestedRevision(state.getRequestedRevision() + 1);
        }
        state.setAttemptCount(0);
        state.setNextAttemptAt(null);
        state.setLastError(null);

        if (!hasActiveLease(state, now)) {
            state.setStatus(PostSearchIndexStatus.PENDING);
            clearLease(state);
        }
        postSearchIndexStateRepo.save(state);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean prepareEnqueue(
            UUID postId,
            UUID indexGeneration) {
        PostSearchIndexStateEntity state = postSearchIndexStateRepo
                .findForUpdateByPostId(postId)
                .orElse(null);
        if (state == null || !requiresWork(
                state,
                indexGeneration,
                LocalDateTime.now())) {
            return false;
        }

        state.setStatus(PostSearchIndexStatus.QUEUED);
        state.setLeaseToken(null);
        state.setLeaseExpiresAt(LocalDateTime.now().plus(QUEUE_TIMEOUT));
        state.setNextAttemptAt(null);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEnqueueFailed(UUID postId) {
        postSearchIndexStateRepo.findForUpdateByPostId(postId)
                .filter(state -> state.getStatus()
                        == PostSearchIndexStatus.QUEUED)
                .ifPresent(state -> {
                    state.setStatus(PostSearchIndexStatus.PENDING);
                    clearLease(state);
                });
    }

    @Override
    @Transactional
    public Optional<PostSearchIndexClaim> claim(
            UUID postId,
            UUID leaseToken) {
        PostSearchIndexStateEntity state = postSearchIndexStateRepo
                .findForUpdateByPostId(postId)
                .orElse(null);
        if (state == null
                || state.getStatus() != PostSearchIndexStatus.QUEUED) {
            return Optional.empty();
        }

        state.setStatus(PostSearchIndexStatus.PROCESSING);
        state.setLeaseToken(leaseToken);
        state.setLeaseExpiresAt(
                LocalDateTime.now().plus(PROCESSING_TIMEOUT));
        return Optional.of(new PostSearchIndexClaim(
                postId,
                state.getRequestedRevision(),
                leaseToken));
    }

    @Override
    @Transactional
    public boolean complete(
            PostSearchIndexClaim claim,
            PostSearchIndexWriteResult writeResult) {
        PostSearchIndexStateEntity state = requireClaimedState(claim);
        state.setProcessedRevision(Math.max(
                state.getProcessedRevision(),
                claim.requestedRevision()));
        state.setIndexedSourceUpdatedAt(
                writeResult.indexedSourceUpdatedAt());
        state.setIndexedModelVersion(writeResult.modelVersion());
        state.setIndexedGeneration(writeResult.indexGeneration());
        state.setAttemptCount(0);
        state.setNextAttemptAt(null);
        state.setLastError(null);
        clearLease(state);

        boolean changedDuringProcessing = state.getRequestedRevision()
                > claim.requestedRevision();
        state.setStatus(changedDuringProcessing
                ? PostSearchIndexStatus.PENDING
                : PostSearchIndexStatus.SYNCED);
        return changedDuringProcessing;
    }

    @Override
    @Transactional
    public boolean fail(
            PostSearchIndexClaim claim,
            String errorMessage) {
        PostSearchIndexStateEntity state = requireClaimedState(claim);
        clearLease(state);

        boolean changedDuringProcessing = state.getRequestedRevision()
                > claim.requestedRevision();
        if (changedDuringProcessing) {
            state.setStatus(PostSearchIndexStatus.PENDING);
            state.setNextAttemptAt(null);
            return true;
        }

        state.setStatus(PostSearchIndexStatus.FAILED);
        state.setAttemptCount(state.getAttemptCount() + 1);
        state.setNextAttemptAt(LocalDateTime.now().plus(RETRY_DELAY));
        state.setLastError(truncateError(errorMessage));
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findRecoveryCandidateIds(
            UUID indexGeneration,
            int limit) {
        return postSearchIndexStateRepo.findRecoveryCandidateIds(
                PostSearchIndexStatus.PENDING,
                PostSearchIndexStatus.FAILED,
                LEASED_STATUSES,
                PostSearchIndexStatus.SYNCED,
                LocalDateTime.now(),
                indexGeneration,
                PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public List<UUID> createBackfillStates(int limit) {
        List<UUID> postIds = postSearchIndexStateRepo.findBackfillPostIds(
                PostLifecycleStatus.ACTIVE,
                PostModerationStatus.PUBLISHED,
                SEARCHABLE_POST_TYPES,
                PageRequest.of(0, limit));
        postIds.forEach(postId -> {
            if (!postSearchIndexStateRepo.existsById(postId)) {
                postSearchIndexStateRepo.save(newState(postId));
            }
        });
        return postIds;
    }

    private PostSearchIndexStateEntity newState(UUID postId) {
        PostSearchIndexStateEntity state = new PostSearchIndexStateEntity();
        state.setPostId(postId);
        state.setStatus(PostSearchIndexStatus.PENDING);
        state.setRequestedRevision(1);
        state.setProcessedRevision(0);
        state.setAttemptCount(0);
        return state;
    }

    private boolean requiresWork(
            PostSearchIndexStateEntity state,
            UUID indexGeneration,
            LocalDateTime now) {
        if (hasActiveLease(state, now)) {
            return false;
        }
        if (state.getRequestedRevision() > state.getProcessedRevision()) {
            return true;
        }
        if (!Objects.equals(
                state.getIndexedGeneration(),
                indexGeneration)) {
            return true;
        }
        return state.getStatus() == PostSearchIndexStatus.FAILED
                && (state.getNextAttemptAt() == null
                        || !state.getNextAttemptAt().isAfter(now));
    }

    private boolean hasActiveLease(
            PostSearchIndexStateEntity state,
            LocalDateTime now) {
        return LEASED_STATUSES.contains(state.getStatus())
                && state.getLeaseExpiresAt() != null
                && state.getLeaseExpiresAt().isAfter(now);
    }

    private PostSearchIndexStateEntity requireClaimedState(
            PostSearchIndexClaim claim) {
        PostSearchIndexStateEntity state = postSearchIndexStateRepo
                .findForUpdateByPostId(claim.postId())
                .orElseThrow(() -> new IllegalStateException(
                        "Post search index state is missing for post ["
                                + claim.postId()
                                + "]."));
        if (state.getStatus() != PostSearchIndexStatus.PROCESSING
                || !Objects.equals(
                        state.getLeaseToken(),
                        claim.leaseToken())) {
            throw new IllegalStateException(
                    "Post search index lease is no longer owned for post ["
                            + claim.postId()
                            + "].");
        }
        return state;
    }

    private void clearLease(PostSearchIndexStateEntity state) {
        state.setLeaseToken(null);
        state.setLeaseExpiresAt(null);
    }

    private String truncateError(String errorMessage) {
        if (errorMessage == null) {
            return "Unknown post search indexing error.";
        }
        return errorMessage.length() <= MAX_ERROR_LENGTH
                ? errorMessage
                : errorMessage.substring(0, MAX_ERROR_LENGTH);
    }
}
