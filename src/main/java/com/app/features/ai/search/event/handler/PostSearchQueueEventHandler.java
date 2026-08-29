package com.app.features.ai.search.event.handler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.features.ai.search.service.PostSearchQueueService;
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

    private final PostSearchQueueService postSearchQueueSvc;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostMutation(PostMutationEvent event) {
        try {
            postSearchQueueSvc.enqueue(event.postId());
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to enqueue search synchronization for post [{}].",
                    event.postId(),
                    exception);
        }
    }
}
