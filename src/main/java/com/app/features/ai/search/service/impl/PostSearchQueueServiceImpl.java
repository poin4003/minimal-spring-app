package com.app.features.ai.search.service.impl;

import java.util.UUID;

import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.search.job.PostSearchSyncJob;
import com.app.features.ai.search.service.PostSearchIndexStateService;
import com.app.features.ai.search.service.PostSearchQueueService;
import com.app.features.ai.search.service.PostVectorIndex;
import com.app.features.ai.search.support.AiSearchCapability;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostSearchQueueServiceImpl implements PostSearchQueueService {

    private final AiSearchCapability aiSearchCapability;
    private final PostSearchIndexStateService postSearchIndexStateSvc;
    private final ObjectProvider<PostVectorIndex> postVectorIndexProvider;
    private final JobScheduler jobScheduler;

    @Override
    public boolean enqueue(UUID postId) {
        if (aiSearchCapability.resolveAvailability()
                != AiAvailability.READY) {
            return false;
        }

        PostVectorIndex postVectorIndex = postVectorIndexProvider
                .getIfAvailable();
        if (postVectorIndex == null || !postSearchIndexStateSvc.prepareEnqueue(
                postId,
                postVectorIndex.getIndexGeneration())) {
            return false;
        }

        try {
            jobScheduler.<PostSearchSyncJob>enqueue(
                    job -> job.execute(postId));
            return true;
        } catch (RuntimeException exception) {
            postSearchIndexStateSvc.markEnqueueFailed(postId);
            throw exception;
        }
    }
}
