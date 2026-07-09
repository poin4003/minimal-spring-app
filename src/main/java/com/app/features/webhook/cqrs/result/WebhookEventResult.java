package com.app.features.webhook.cqrs.result;

import java.util.UUID;

import lombok.Data;

@Data
public class WebhookEventResult {

    private UUID id;

    private String eventType;

    private String businessReference;

    private Object payload;
}
