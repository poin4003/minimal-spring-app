package com.app.features.webhook.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.base.BaseEntity;
import com.app.features.webhook.enums.WebhookSubscriptionStatus;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "webhook_subscription")
@Data
@EqualsAndHashCode(callSuper = true)
public class WebhookSubscriptionEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "status", nullable = false)
    private WebhookSubscriptionStatus status;

    @Column(name = "partner_code", nullable = false, length = 50)
    private String partnerCode = "DEFAULT";

    @Column(name = "max_rpm", nullable = false)
    private Integer maxRpm = 60;

    @Column(name = "base_delay_seconds", nullable = false)
    private Integer baseDelaySeconds = 30;

    @Column(name = "max_delay_seconds", nullable = false)
    private Integer maxDelaySeconds = 30;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 5;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb", nullable = false)
    private JsonNode config;
}
