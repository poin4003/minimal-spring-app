package com.app.features.post.aimoderation.support;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.post.aimoderation.enums.PostAiModerationAvailability;
import com.app.features.post.aimoderation.service.PostAiModerationHealthClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostAiModerationCapability {

    private final AppProperties appProperties;
    private final ObjectProvider<PostAiModerationHealthClient>
            postAiModerationHealthClientProvider;

    public boolean isEnabled() {
        return appProperties.getPost()
                .getAiModeration()
                .isEnabled();
    }

    public PostAiModerationAvailability resolveAvailability() {
        if (!isEnabled()) {
            return PostAiModerationAvailability.DISABLED;
        }

        PostAiModerationHealthClient healthClient =
                postAiModerationHealthClientProvider.getIfAvailable();
        return healthClient != null && healthClient.isReady()
                ? PostAiModerationAvailability.READY
                : PostAiModerationAvailability.UNAVAILABLE;
    }
}
