package com.app.features.auth.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.core.enums.AppLanguage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "registration", indexes = {
        @Index(
                name = "uk_registration_email",
                columnList = "email",
                unique = true),
        @Index(
                name = "uk_registration_completion_token_hash",
                columnList = "completion_token_hash",
                unique = true),
        @Index(
                name = "idx_registration_otp_expires_at",
                columnList = "otp_expires_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class RegistrationEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "code_hash", length = 64)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "language", nullable = false)
    private AppLanguage language = AppLanguage.EN;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "otp_expires_at")
    private Instant otpExpiresAt;

    @Column(name = "resend_available_at")
    private Instant resendAvailableAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "completion_token_hash", length = 64)
    private String completionTokenHash;

    @Column(name = "completion_expires_at")
    private Instant completionExpiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
