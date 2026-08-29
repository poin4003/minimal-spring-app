package com.app.features.ai.search.event.handler;

import org.jobrunr.scheduling.JobScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.search.job.PostSearchSyncJob;
import com.app.features.ai.search.support.AiSearchCapability;
import com.app.features.post.event.PostMutationEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.ai.search",
        name = "enabled",
        havingValue = "true")
public class PostSearchQueueEventHandler {

    private final JobScheduler jobScheduler;
    private final AiSearchCapability aiSearchCapability;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostMutation(PostMutationEvent event) {
        if (aiSearchCapability.resolveAvailability()
                != AiAvailability.READY) {
            log.warn(
                    "Skipping search synchronization for post [{}]: {}.",
                    event.postId(),
                    aiSearchCapability.resolveStatusDetail());
            return;
        }

        try {
            jobScheduler.<PostSearchSyncJob>enqueue(
                    job -> job.execute(event.postId()));
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to enqueue search synchronization for post [{}].",
                    event.postId(),
                    exception);
        }
    }
}
