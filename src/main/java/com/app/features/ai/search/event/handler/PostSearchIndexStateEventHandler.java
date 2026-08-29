package com.app.features.ai.search.event.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.features.ai.search.service.PostSearchIndexStateService;
import com.app.features.post.event.PostMutationEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostSearchIndexStateEventHandler {

    private final PostSearchIndexStateService postSearchIndexStateSvc;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePostMutation(PostMutationEvent event) {
        postSearchIndexStateSvc.markDirty(event.postId());
    }
}
