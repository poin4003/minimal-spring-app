package com.app.features.post.videopost.entity;

import java.util.UUID;

import com.app.core.db.BaseAuditEntity;
import com.app.features.media.entity.MediaEntity;
import com.app.features.user.entity.UserBaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "video_series", indexes = {
        @Index(
                name = "idx_video_series_owner_created_at",
                columnList = "owner_id, created_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class VideoSeriesEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserBaseEntity owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_media_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MediaEntity coverMedia;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @PositiveOrZero
    @Column(name = "video_count", nullable = false)
    private int videoCount;
}
