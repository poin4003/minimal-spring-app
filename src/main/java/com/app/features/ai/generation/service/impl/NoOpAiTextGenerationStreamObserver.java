package com.app.features.ai.generation.service.impl;

import com.app.features.ai.generation.service.AiTextGenerationStreamObserver;

public final class NoOpAiTextGenerationStreamObserver
        implements AiTextGenerationStreamObserver {

    public static final NoOpAiTextGenerationStreamObserver INSTANCE =
            new NoOpAiTextGenerationStreamObserver();

    private NoOpAiTextGenerationStreamObserver() {
    }

    @Override
    public void onToken(String token) {
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
