package com.app.features.webhook.worker;

import com.app.features.webhook.enums.WebhookDecision;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebhookRuleResult {
    private final WebhookDecision decision;
    private final Integer httpStatusCode;
    private final String errorMessage;
}
