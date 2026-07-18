package com.app.features.user.entity;

import java.util.UUID;

import com.app.core.db.BaseAuditEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "user_info", indexes = {
        @Index(name = "idx_user_info_created_at", columnList = "created_at"),
        @Index(name = "idx_user_info_updated_at", columnList = "updated_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class UserInfoEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private UserBaseEntity user;

    @Column(name = "username")
    private String username;

    @Column(name = "phone_number")
    private String phoneNumber;
}
