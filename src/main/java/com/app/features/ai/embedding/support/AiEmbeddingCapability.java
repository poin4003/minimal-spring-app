package com.app.features.ai.embedding.support;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.ai.embedding.service.AiEmbeddingHealthClient;
import com.app.features.ai.enums.AiAvailability;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiEmbeddingCapability {

    private final AppProperties appProperties;
    private final ObjectProvider<AiEmbeddingHealthClient>
            aiEmbeddingHealthClientProvider;

    public boolean isEnabled() {
        return appProperties.getAi().getEmbedding().isEnabled();
    }

    public AiAvailability resolveAvailability() {
        if (!isEnabled()) {
            return AiAvailability.DISABLED;
        }

        AiEmbeddingHealthClient healthClient =
                aiEmbeddingHealthClientProvider.getIfAvailable();
        return healthClient != null && healthClient.isReady()
                ? AiAvailability.READY
                : AiAvailability.UNAVAILABLE;
    }

    public String resolveRuntimeProvider() {
        if (!isEnabled()) {
            return AiAvailability.DISABLED.name();
        }
        AiEmbeddingHealthClient healthClient =
                aiEmbeddingHealthClientProvider.getIfAvailable();
        return healthClient == null
                ? AiAvailability.UNAVAILABLE.name()
                : healthClient.getRuntimeProvider();
    }
}
