package com.app.features.ai.generation.service;

public interface AiTextGenerationStreamObserver {

    void onToken(String token);

    boolean isCancelled();
}
