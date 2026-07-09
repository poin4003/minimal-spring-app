package com.app.features.auth.entity;

import java.time.Instant;
import java.util.UUID;

import com.app.core.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "consumed_refresh_token", indexes = {
        @Index(name = "idx_consumed_token_value", columnList = "token_value"),
        @Index(name = "idx_comsumed_user", columnList = "user_id")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class ConsumedRefreshTokenEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "key_store_id", nullable = false)
    private UUID keyStoreId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_value")
    private String tokenValue;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @Column(name = "used_at")
    private Instant usedAt;
}
