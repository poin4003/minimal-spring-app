package com.app.features.ai.rag.service.impl;

import com.app.features.ai.rag.schema.model.PostRagStreamMetadata;
import com.app.features.ai.rag.service.PostRagStreamObserver;

public final class NoOpPostRagStreamObserver
        implements PostRagStreamObserver {

    public static final NoOpPostRagStreamObserver INSTANCE =
            new NoOpPostRagStreamObserver();

    private NoOpPostRagStreamObserver() {
    }

    @Override
    public void onMetadata(PostRagStreamMetadata metadata) {
    }

    @Override
    public void onToken(String token) {
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
