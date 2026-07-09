package com.app.features.webhook.api.v1.dto.request;

import com.app.features.webhook.enums.WebhookSubscriptionStatus;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateWebhookSubscriptionDto {

    private String url;

    private WebhookSubscriptionStatus status;

    @Min(value = 1, message = "Speed send api (request per minute) must be more than or equal 1")
    private Integer maxRpm;

    @Min(value = 1, message = "Time to delay must be more than or equal 1")
    private Integer baseDelaySeconds;

    @Min(value = 1, message = "Number of retries must be more than or equal 1")
    private Integer maxRetries;

    private JsonNode config;
}
