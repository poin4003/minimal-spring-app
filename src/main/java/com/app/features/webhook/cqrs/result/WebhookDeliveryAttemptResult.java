package com.app.features.webhook.cqrs.result;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.webhook.enums.WebhookDeliveryAttemptStatus;

import lombok.Data;

@Data
public class WebhookDeliveryAttemptResult {

    private UUID id;

    private String endpointUrl;

    private WebhookDeliveryAttemptStatus status;

    private int attemptCount;

    private Integer httpStatus;

    private String errorMessage;

    private LocalDateTime nextRetryAt;
}
