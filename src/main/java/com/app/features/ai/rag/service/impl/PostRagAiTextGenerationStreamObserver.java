package com.app.features.ai.rag.service.impl;

import java.util.Objects;

import com.app.features.ai.generation.service.AiTextGenerationStreamObserver;
import com.app.features.ai.rag.service.PostRagStreamObserver;

public final class PostRagAiTextGenerationStreamObserver
        implements AiTextGenerationStreamObserver {

    private final PostRagStreamObserver postRagStreamObserver;

    public PostRagAiTextGenerationStreamObserver(
            PostRagStreamObserver postRagStreamObserver) {
        this.postRagStreamObserver = Objects.requireNonNull(
                postRagStreamObserver,
                "postRagStreamObserver must not be null");
    }

    @Override
    public void onToken(String token) {
        postRagStreamObserver.onToken(token);
    }

    @Override
    public boolean isCancelled() {
        return Thread.currentThread().isInterrupted()
                || postRagStreamObserver.isCancelled();
    }
}
