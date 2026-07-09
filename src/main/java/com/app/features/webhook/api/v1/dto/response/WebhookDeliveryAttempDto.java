package com.app.features.webhook.api.v1.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.webhook.enums.WebhookDeliveryAttemptStatus;

import lombok.Data;

@Data
public class WebhookDeliveryAttempDto {

    private UUID id;

    private String endpointUrl;

    private WebhookDeliveryAttemptStatus status;

    private int attemptCount;

    private Integer httpStatus;

    private String errorMessage;

    private LocalDateTime nextRetryAt;
}
