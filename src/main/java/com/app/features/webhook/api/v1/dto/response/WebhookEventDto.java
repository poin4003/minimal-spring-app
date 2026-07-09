package com.app.features.webhook.api.v1.dto.response;

import java.util.UUID;

import lombok.Data;

@Data
public class WebhookEventDto {

    private UUID id;

    private String eventType;

    private String businessReference;

    private Object payload;
}
