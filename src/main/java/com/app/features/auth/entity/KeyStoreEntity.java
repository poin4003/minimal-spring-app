package com.app.features.auth.entity;

import java.util.UUID;

import com.app.core.db.BaseAuditEntity;

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
@Table(name = "key_store", indexes = {
        @Index(name = "idx_keystore_user", columnList = "user_id"),
        @Index(name = "uk_keystore_token", columnList = "refresh_token", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
public class KeyStoreEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "signing_key", nullable = false)
    private String signingKey;

    @Column(name = "refresh_token")
    private String refreshToken;
}
