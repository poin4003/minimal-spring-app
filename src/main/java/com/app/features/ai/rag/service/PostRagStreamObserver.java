package com.app.features.ai.rag.service;

import com.app.features.ai.rag.schema.model.PostRagStreamMetadata;

public interface PostRagStreamObserver {

    void onMetadata(PostRagStreamMetadata metadata);

    void onToken(String token);

    boolean isCancelled();
}
