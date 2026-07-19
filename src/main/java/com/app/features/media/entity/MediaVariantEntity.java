package com.app.features.media.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.features.media.enums.MediaVariantType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "media_variant", indexes = {
        @Index(name = "uk_media_variant_storage_key", columnList = "storage_key", unique = true),
        @Index(name = "uk_media_variant_media_type", columnList = "media_id, variant_type", unique = true),
        @Index(name = "idx_media_variant_created_at", columnList = "created_at"),
        @Index(name = "idx_media_variant_updated_at", columnList = "updated_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class MediaVariantEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MediaEntity media;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "variant_type", nullable = false)
    private MediaVariantType variantType;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    private Integer width;

    private Integer height;

    private Integer bitrate;
}
