package com.app.features.media.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.features.media.enums.MediaUploadStatus;
import com.app.features.user.entity.UserBaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "media_upload_session", indexes = {
        @Index(name = "idx_media_upload_created_by_status", columnList = "created_by, status"),
        @Index(name = "idx_media_upload_expires_at", columnList = "expires_at"),
        @Index(name = "idx_media_upload_created_at", columnList = "created_at"),
        @Index(name = "idx_media_upload_updated_at", columnList = "updated_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class MediaUploadSessionEntity extends BaseAuditEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserBaseEntity createdBy;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "chunk_size", nullable = false)
    private int chunkSize;

    @Column(name = "total_chunks", nullable = false)
    private int totalChunks;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private MediaUploadStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_media_id", unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MediaEntity completedMedia;
}
