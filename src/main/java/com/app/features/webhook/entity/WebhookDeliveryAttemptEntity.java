package com.app.features.webhook.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.core.db.BaseEntity;
import com.app.features.webhook.enums.WebhookDeliveryAttemptStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "webhook_delivery_attempt")
@Data
@EqualsAndHashCode(callSuper = true)
public class WebhookDeliveryAttemptEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private WebhookDeliveryAttemptStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_http_status")
    private Integer lastHttpStatus;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private WebhookEventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private WebhookSubscriptionEntity subscription;
}
