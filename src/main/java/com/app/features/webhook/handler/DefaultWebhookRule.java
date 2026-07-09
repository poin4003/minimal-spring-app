package com.app.features.webhook.handler;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.app.features.webhook.entity.WebhookEventEntity;
import com.app.features.webhook.entity.WebhookSubscriptionEntity;
import com.app.features.webhook.enums.WebhookDecision;
import com.app.features.webhook.http.BaseApiRequest;
import com.app.features.webhook.http.WebhookHttpClient;
import com.app.features.webhook.http.WebhookHttpResponse;
import com.app.features.webhook.worker.WebhookRuleResult;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultWebhookRule implements WebhookPartnerRule {

    private final WebhookHttpClient httpClient;

    @Override
    public String getPartnerCode() {
        return "DEFAULT";
    }

    @Override
    public WebhookRuleResult execute(WebhookEventEntity event, WebhookSubscriptionEntity subscription) {
        log.debug("Executing DEFAULT webhook rule for subscription: {}", subscription.getId());

        JsonNode rawPayload = event.getPayload();

        var requestBuilder = BaseApiRequest.<JsonNode>builder()
                .url(subscription.getUrl())
                .method(HttpMethod.POST)
                .payload(rawPayload);

        BaseApiRequest<JsonNode> request = requestBuilder.build();

        WebhookHttpResponse<Void> response = httpClient.send(request, Void.class);

        WebhookDecision decision = evaluate(response);

        return WebhookRuleResult.builder()
                .decision(decision)
                .httpStatusCode(response.getStatusCode())
                .errorMessage(response.getRawBody())
                .build();
    }
}
