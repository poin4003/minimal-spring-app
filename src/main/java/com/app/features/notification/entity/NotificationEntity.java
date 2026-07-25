package com.app.features.notification.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.features.notification.enums.NotificationResourceType;
import com.app.features.notification.enums.NotificationType;
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
@Table(name = "notification", indexes = {
        @Index(
                name = "uk_notification_recipient_type_resource",
                columnList = "recipient_id, notification_type, resource_type, resource_id",
                unique = true),
        @Index(
                name = "idx_notification_recipient_read_created",
                columnList = "recipient_id, read_at, created_at"),
        @Index(
                name = "idx_notification_resource",
                columnList = "resource_type, resource_id"),
        @Index(name = "idx_notification_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserBaseEntity recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserBaseEntity actor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "resource_type", nullable = false)
    private NotificationResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
