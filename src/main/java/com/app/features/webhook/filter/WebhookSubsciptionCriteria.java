package com.app.features.webhook.filter;

import com.app.features.webhook.enums.WebhookSubscriptionStatus;

public interface WebhookSubsciptionCriteria {
    String getPartnerCode();

    WebhookSubscriptionStatus getStatus();
}
