package com.app.features.samplepartner.executor;

import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import com.app.core.constant.PartnerConstants;
import com.app.core.utils.JsonNodeConverter;
import com.app.features.samplepartner.config.SamplePartnerConfig;
import com.app.features.samplepartner.dto.request.SampleLoginRequest;
import com.app.features.samplepartner.dto.response.SampleApiResponse;
import com.app.features.samplepartner.dto.response.SampleLoginMetadata;
import com.app.features.webhook.entity.WebhookEventEntity;
import com.app.features.webhook.entity.WebhookSubscriptionEntity;
import com.app.features.webhook.enums.WebhookDecision;
import com.app.features.webhook.handler.WebhookPartnerRule;
import com.app.features.webhook.http.BaseApiRequest;
import com.app.features.webhook.http.WebhookHttpClient;
import com.app.features.webhook.http.WebhookHttpResponse;
import com.app.features.webhook.worker.WebhookRuleResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SamplePartnerExecutor implements WebhookPartnerRule {

    private final JsonNodeConverter jsonNodeConverter;
    private final WebhookHttpClient httpClient;

    @Override
    public String getPartnerCode() {
        return PartnerConstants.SAMPLE_PARTNER;
    }

    @Override
    public WebhookDecision evaluate(WebhookHttpResponse<?> response) {
        if (response.isNetworkError() || response.getStatusCode() == null)
            return WebhookDecision.RETRY;

        int status = response.getStatusCode();
        if (status == 429)
            return WebhookDecision.RETRY;
        if (status >= 200 && status < 300)
            return WebhookDecision.SUCCESS;
        if (status >= 500)
            return WebhookDecision.RETRY;

        return WebhookDecision.FAIL_FAST;
    }

    @Override
    public WebhookRuleResult execute(WebhookEventEntity event, WebhookSubscriptionEntity subscription) {
        SamplePartnerConfig config = jsonNodeConverter.fromNode(subscription.getConfig(), SamplePartnerConfig.class);
        String baseUrl = subscription.getUrl();
        String eventType = event.getEventType();

        WebhookHttpResponse<?> apiResponse;

        switch (eventType) {
            case "VENUE_CREATED":
                apiResponse = executeWithAuth(baseUrl, config, (token) -> 
                    createVenue(baseUrl, token, event.getPayload())
                );
                break;

            default:
                return WebhookRuleResult.builder()
                    .decision(WebhookDecision.FAIL_FAST)
                    .errorMessage("Unsuported event type: " + eventType)
                    .build();
        }

        return WebhookRuleResult.builder()
            .decision(evaluate(apiResponse))
            .httpStatusCode(apiResponse.getStatusCode())
            .errorMessage(apiResponse.getRawBody())
            .build();
    }

    private WebhookHttpResponse<SampleApiResponse<SampleLoginMetadata>> login(String baseUrl,
            SamplePartnerConfig config) {
        String url = baseUrl + "/v1/api/user/login";
        SampleLoginRequest payload = new SampleLoginRequest(config.getUsername(), config.getPassword());

        BaseApiRequest<SampleLoginRequest> request = BaseApiRequest.<SampleLoginRequest>builder()
                .url(url)
                .payload(payload)
                .build();

        return httpClient.send(request, new TypeReference<SampleApiResponse<SampleLoginMetadata>>() {
        });
    }

    private WebhookHttpResponse<JsonNode> createVenue(String baseUrl, String token, JsonNode payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(Objects.requireNonNull(token));

        BaseApiRequest<JsonNode> request = new BaseApiRequest<>();
        request.setUrl(baseUrl + "/v1/api/venue");
        request.setMethod(HttpMethod.POST);
        request.setHeaders(headers);
        request.setPayload(payload);

        return httpClient.send(request, JsonNode.class);
    }

    private WebhookHttpResponse<?> executeWithAuth(String baseUrl, SamplePartnerConfig config,
            java.util.function.Function<String, WebhookHttpResponse<?>> action) {

        WebhookHttpResponse<SampleApiResponse<SampleLoginMetadata>> loginRes = login(baseUrl, config);

        if (evaluate(loginRes) != WebhookDecision.SUCCESS) {
            return loginRes;
        }

        String token = loginRes.getData().getMetadata().getToken();

        return action.apply(token);
    }
}
