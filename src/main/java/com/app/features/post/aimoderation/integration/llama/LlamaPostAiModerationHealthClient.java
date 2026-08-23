package com.app.features.post.aimoderation.integration.llama;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.app.config.settings.AppProperties;
import com.app.features.post.aimoderation.service.PostAiModerationHealthClient;

@Service
@ConditionalOnProperty(
        prefix = "app.post.ai-moderation",
        name = "enabled",
        havingValue = "true")
public class LlamaPostAiModerationHealthClient
        implements PostAiModerationHealthClient {

    private final RestClient restClient;

    public LlamaPostAiModerationHealthClient(
            RestClient.Builder restClientBuilder,
            AppProperties appProperties) {
        int timeoutMillis = Math.toIntExact(
                appProperties.getPost()
                        .getAiModeration()
                        .getMachine()
                        .getHealthTimeout()
                        .toMillis());

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);

        this.restClient = restClientBuilder
                .baseUrl(appProperties.getPost()
                        .getAiModeration()
                        .getMachine()
                        .getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean isReady() {
        try {
            restClient.get()
                    .uri("/health")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            return false;
        }
    }
}
