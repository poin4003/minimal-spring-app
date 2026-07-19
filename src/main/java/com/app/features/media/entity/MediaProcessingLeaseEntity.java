package com.app.features.media.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.core.db.BaseAuditEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "media_processing_lease", indexes = {
        @Index(name = "idx_media_processing_lease_expires_at", columnList = "expires_at"),
        @Index(name = "idx_media_processing_lease_created_at", columnList = "created_at"),
        @Index(name = "idx_media_processing_lease_updated_at", columnList = "updated_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class MediaProcessingLeaseEntity extends BaseAuditEntity {

    @Id
    @Column(name = "media_id")
    private UUID mediaId;

    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
