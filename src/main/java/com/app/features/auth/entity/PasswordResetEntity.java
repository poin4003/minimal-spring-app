package com.app.features.auth.entity;

import java.time.Instant;
import java.util.UUID;

import com.app.core.db.BaseAuditEntity;
import com.app.features.user.entity.UserBaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "password_reset", indexes = {
        @Index(
                name = "uk_password_reset_user_id",
                columnList = "user_id",
                unique = true),
        @Index(
                name = "uk_password_reset_token_hash",
                columnList = "reset_token_hash",
                unique = true),
        @Index(
                name = "idx_password_reset_updated_at",
                columnList = "updated_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class PasswordResetEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserBaseEntity user;

    @ToString.Exclude
    @Column(name = "code_hash", length = 64)
    private String codeHash;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "otp_expires_at")
    private Instant otpExpiresAt;

    @Column(name = "resend_available_at")
    private Instant resendAvailableAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @ToString.Exclude
    @Column(name = "reset_token_hash", length = 64)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private Instant resetTokenExpiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
