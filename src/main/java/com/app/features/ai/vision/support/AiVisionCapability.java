package com.app.features.ai.vision.support;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.vision.service.AiVisionHealthClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiVisionCapability {

    private final AppProperties appProperties;
    private final ObjectProvider<AiVisionHealthClient>
            aiVisionHealthClientProvider;

    public boolean isEnabled() {
        return appProperties.getAi().getVision().isEnabled();
    }

    public AiAvailability resolveAvailability() {
        if (!isEnabled()) {
            return AiAvailability.DISABLED;
        }

        AiVisionHealthClient healthClient =
                aiVisionHealthClientProvider.getIfAvailable();
        return healthClient != null && healthClient.isReady()
                ? AiAvailability.READY
                : AiAvailability.UNAVAILABLE;
    }

    public String resolveRuntimeProvider() {
        if (!isEnabled()) {
            return AiAvailability.DISABLED.name();
        }
        AiVisionHealthClient healthClient =
                aiVisionHealthClientProvider.getIfAvailable();
        return healthClient == null
                ? AiAvailability.UNAVAILABLE.name()
                : healthClient.getRuntimeProvider();
    }
}
