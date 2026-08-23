package com.app.features.post.aimoderation.event.handler;

import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.features.post.aimoderation.enums.PostAiModerationMode;
import com.app.features.post.aimoderation.job.PostAiModerationJob;
import com.app.features.post.aimoderation.service.PostAiModerationConfigService;
import com.app.features.post.event.PostSubmittedForReviewEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostAiModerationQueueEventHandler {

    private final JobScheduler jobScheduler;
    private final PostAiModerationConfigService postAiModerationConfigSvc;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostSubmittedForReview(
            PostSubmittedForReviewEvent event) {
        try {
            if (postAiModerationConfigSvc.requireCurrentConfig().getMode()
                    != PostAiModerationMode.AUTO) {
                return;
            }

            jobScheduler.<PostAiModerationJob>enqueue(
                    job -> job.execute(event.postId()));
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to enqueue AI moderation for post [{}].",
                    event.postId(),
                    exception);
        }
    }
}
