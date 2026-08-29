package com.app.features.ai.search.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.app.config.settings.AppProperties;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.search.schema.model.PostSearchReconciliationResult;
import com.app.features.ai.search.service.PostSearchIndexStateService;
import com.app.features.ai.search.service.PostSearchQueueService;
import com.app.features.ai.search.service.PostSearchReconciliationService;
import com.app.features.ai.search.service.PostVectorIndex;
import com.app.features.ai.search.support.AiSearchCapability;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostSearchReconciliationServiceImpl
        implements PostSearchReconciliationService {

    private final AppProperties appProperties;
    private final AiSearchCapability aiSearchCapability;
    private final ObjectProvider<PostVectorIndex> postVectorIndexProvider;
    private final PostSearchIndexStateService postSearchIndexStateSvc;
    private final PostSearchQueueService postSearchQueueSvc;

    @Override
    public PostSearchReconciliationResult reconcile() {
        AiAvailability availability = aiSearchCapability
                .resolveAvailability();
        if (availability != AiAvailability.READY) {
            return PostSearchReconciliationResult.skipped(availability);
        }

        PostVectorIndex postVectorIndex = postVectorIndexProvider
                .getIfAvailable();
        if (postVectorIndex == null) {
            return PostSearchReconciliationResult.skipped(
                    AiAvailability.UNAVAILABLE);
        }

        int batchSize = appProperties.getAi()
                .getSearch()
                .getReconciliationBatchSize();
        List<UUID> recoveryCandidates = postSearchIndexStateSvc
                .findRecoveryCandidateIds(
                        postVectorIndex.getIndexGeneration(),
                        batchSize);
        MutableReconciliationStats stats = new MutableReconciliationStats();
        stats.recoveryCandidates = recoveryCandidates.size();
        enqueue(recoveryCandidates, stats);

        int remainingCapacity = batchSize - recoveryCandidates.size();
        if (remainingCapacity > 0) {
            List<UUID> backfillPostIds = postSearchIndexStateSvc
                    .createBackfillStates(remainingCapacity);
            stats.backfillStatesCreated = backfillPostIds.size();
            enqueue(backfillPostIds, stats);
        }

        return stats.toResult();
    }

    private void enqueue(
            List<UUID> postIds,
            MutableReconciliationStats stats) {
        postIds.forEach(postId -> {
            try {
                if (postSearchQueueSvc.enqueue(postId)) {
                    stats.jobsEnqueued++;
                }
            } catch (RuntimeException exception) {
                stats.failures++;
                log.error(
                        "Unable to enqueue search reconciliation for post [{}].",
                        postId,
                        exception);
            }
        });
    }

    private static final class MutableReconciliationStats {

        private int recoveryCandidates;
        private int backfillStatesCreated;
        private int jobsEnqueued;
        private int failures;

        private PostSearchReconciliationResult toResult() {
            return new PostSearchReconciliationResult(
                    AiAvailability.READY,
                    recoveryCandidates,
                    backfillStatesCreated,
                    jobsEnqueued,
                    failures);
        }
    }
}
