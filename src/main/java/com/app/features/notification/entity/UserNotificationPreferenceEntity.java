package com.app.features.notification.entity;

import java.util.UUID;

import com.app.core.db.BaseAuditEntity;
import com.app.features.user.entity.UserBaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "user_notification_preference")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserNotificationPreferenceEntity extends BaseAuditEntity {

    @Id
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserBaseEntity user;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;
}
