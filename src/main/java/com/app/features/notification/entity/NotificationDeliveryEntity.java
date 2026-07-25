package com.app.features.notification.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.features.notification.enums.NotificationChannel;
import com.app.features.notification.enums.NotificationDeliveryStatus;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "notification_delivery", indexes = {
        @Index(
                name = "uk_notification_delivery_notification_channel",
                columnList = "notification_id, channel",
                unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationDeliveryEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private NotificationEntity notification;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(name = "recipient_address", nullable = false, length = 512)
    private String recipientAddress;

    @Column(name = "subject_snapshot", length = 500)
    private String subjectSnapshot;

    @Lob
    @Column(name = "content_snapshot", nullable = false)
    private String contentSnapshot;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private NotificationDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
