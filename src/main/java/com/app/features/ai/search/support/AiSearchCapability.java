package com.app.features.ai.search.support;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.search.service.AiSearchHealthClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiSearchCapability {

    private final AppProperties appProperties;
    private final ObjectProvider<AiSearchHealthClient>
            aiSearchHealthClientProvider;

    public boolean isEnabled() {
        return appProperties.getAi().getSearch().isEnabled();
    }

    public AiAvailability resolveAvailability() {
        if (!isEnabled()) {
            return AiAvailability.DISABLED;
        }

        AiSearchHealthClient healthClient =
                aiSearchHealthClientProvider.getIfAvailable();
        return healthClient != null && healthClient.isReady()
                ? AiAvailability.READY
                : AiAvailability.UNAVAILABLE;
    }

    public String resolveStatusDetail() {
        if (!isEnabled()) {
            return AiAvailability.DISABLED.name();
        }

        AiSearchHealthClient healthClient =
                aiSearchHealthClientProvider.getIfAvailable();
        return healthClient == null
                ? AiAvailability.UNAVAILABLE.name()
                : healthClient.getStatusDetail();
    }
}
