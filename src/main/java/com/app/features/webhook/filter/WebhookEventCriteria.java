package com.app.features.webhook.filter;

public interface WebhookEventCriteria {
    String getEventType();

    String getBusinessReference();
}
