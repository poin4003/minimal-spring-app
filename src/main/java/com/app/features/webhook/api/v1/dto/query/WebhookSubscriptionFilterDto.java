package com.app.features.webhook.api.v1.dto.query;

import com.app.features.webhook.enums.WebhookSubscriptionStatus;
import com.app.features.webhook.filter.WebhookSubsciptionCriteria;

import lombok.Data;

@Data
public class WebhookSubscriptionFilterDto implements WebhookSubsciptionCriteria {

    private String partnerCode;

    private WebhookSubscriptionStatus status;
}
