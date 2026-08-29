package com.app.features.ai.search.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.features.ai.search.enums.PostSearchIndexStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "post_search_index_state", indexes = {
        @Index(
                name = "idx_post_search_state_status_retry",
                columnList = "status, next_attempt_at, updated_at"),
        @Index(
                name = "idx_post_search_state_status_lease",
                columnList = "status, lease_expires_at, updated_at"),
        @Index(
                name = "idx_post_search_state_status_generation",
                columnList = "status, indexed_generation, updated_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class PostSearchIndexStateEntity extends BaseAuditEntity {

    @Id
    @Column(name = "post_id")
    private UUID postId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private PostSearchIndexStatus status;

    @Column(name = "requested_revision", nullable = false)
    private long requestedRevision;

    @Column(name = "processed_revision", nullable = false)
    private long processedRevision;

    @Column(name = "indexed_source_updated_at")
    private LocalDateTime indexedSourceUpdatedAt;

    @Column(name = "indexed_model_version", length = 255)
    private String indexedModelVersion;

    @Column(name = "indexed_generation")
    private UUID indexedGeneration;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "lease_token")
    private UUID leaseToken;

    @Column(name = "lease_expires_at")
    private LocalDateTime leaseExpiresAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;
}
