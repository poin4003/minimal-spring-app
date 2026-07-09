package com.app.features.webhook.enums;

import com.app.core.annotation.DatabaseEnum;
import com.app.core.base.BaseEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@DatabaseEnum
public enum WebhookDeliveryAttemptStatus implements BaseEnum {
    PENDING(1, "Pending"),
    SUCCESS(2, "Success"),
    FAILED(3, "Failed"),
    EXHAUSTED(4, "Exhausted");

    private final int code;
    private final String description;
}
