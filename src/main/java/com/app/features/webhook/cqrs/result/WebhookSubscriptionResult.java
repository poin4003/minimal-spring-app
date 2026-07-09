package com.app.features.webhook.cqrs.result;

import java.util.UUID;

import com.app.features.webhook.enums.WebhookSubscriptionStatus;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class WebhookSubscriptionResult {

    private UUID id;

    private UUID partnerId;

    private String url;

    private WebhookSubscriptionStatus status;

    private String partnerCode;

    private Integer maxRpm;

    private Integer baseDelaySeconds;

    private Integer maxDelaySeconds;

    private Integer maxRetries;

    private JsonNode config;
}
