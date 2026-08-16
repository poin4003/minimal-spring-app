package com.app.features.post.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.user.entity.UserBaseEntity;

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
@Table(name = "post", indexes = {
        @Index(
                name = "idx_post_lifecycle_moderation_published_at",
                columnList = "lifecycle_status, moderation_status, published_at"),
        @Index(
                name = "idx_post_author_lifecycle_created_at",
                columnList = "author_id, lifecycle_status, created_at"),
        @Index(
                name = "idx_post_lifecycle_deleted_at",
                columnList = "lifecycle_status, deleted_at"),
        @Index(
                name = "idx_post_lifecycle_moderation_moderated_at",
                columnList = "lifecycle_status, moderation_status, moderated_at"),
        @Index(
                name = "idx_post_type_lifecycle_moderation_published_at",
                columnList = "type, lifecycle_status, moderation_status, published_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class PostEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserBaseEntity author;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false)
    private PostType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "lifecycle_status", nullable = false)
    private PostLifecycleStatus lifecycleStatus = PostLifecycleStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "moderation_status")
    private PostModerationStatus moderationStatus;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserBaseEntity moderatedBy;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
