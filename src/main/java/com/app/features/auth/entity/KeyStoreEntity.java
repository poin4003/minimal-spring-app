package com.app.features.auth.entity;

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
@Table(name = "key_store", indexes = {
        @Index(name = "idx_keystore_user", columnList = "user_id", unique = true),
        @Index(name = "idx_keystore_token", columnList = "refresh_token")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class KeyStoreEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "public_key")
    private String publicKey;

    @Column(name = "private_key")
    private String privateKey;

    @Column(name = "refresh_token")
    private String refreshToken;
}
