package com.app.features.post.aimoderation.event.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.features.post.aimoderation.enums.PostAiModerationMode;
import com.app.features.post.aimoderation.service.PostAiModerationConfigService;
import com.app.features.post.event.PostSubmittedForReviewEvent;
import com.app.features.post.moderation.service.PostModerationCommandService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostDirectPublishEventHandler {

    private final PostAiModerationConfigService postAiModerationConfigSvc;
    private final PostModerationCommandService postModerationCommandSvc;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePostSubmittedForReview(
            PostSubmittedForReviewEvent event) {
        if (postAiModerationConfigSvc.requireCurrentConfig().getMode()
                != PostAiModerationMode.DIRECT_PUBLISH) {
            return;
        }

        postModerationCommandSvc.publishPostDirectly(event.postId());
    }
}
