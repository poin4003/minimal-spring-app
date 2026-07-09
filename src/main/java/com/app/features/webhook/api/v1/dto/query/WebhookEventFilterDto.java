package com.app.features.webhook.api.v1.dto.query;

import lombok.Data;

@Data
public class WebhookEventFilterDto {

    private String eventType;

    private String businessReference;   
}
