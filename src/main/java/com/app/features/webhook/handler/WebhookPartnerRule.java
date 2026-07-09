package com.app.features.webhook.handler;

import com.app.features.webhook.entity.WebhookEventEntity;
import com.app.features.webhook.entity.WebhookSubscriptionEntity;
import com.app.features.webhook.enums.WebhookDecision;
import com.app.features.webhook.http.WebhookHttpResponse;
import com.app.features.webhook.worker.WebhookRuleResult;

public interface WebhookPartnerRule {

    String getPartnerCode();

    default WebhookDecision evaluate(WebhookHttpResponse<?> response) {
        if (response.isNetworkError() || response.getStatusCode() == null)
            return WebhookDecision.RETRY;

        int status = response.getStatusCode();
        if (status >= 200 && status < 309)
            return WebhookDecision.SUCCESS;
        if (status >= 500 || status == 409)
            return WebhookDecision.RETRY;

        return WebhookDecision.FAIL_FAST;
    };

    WebhookRuleResult execute(WebhookEventEntity event, WebhookSubscriptionEntity subscription);
}
