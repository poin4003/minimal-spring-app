package com.app.features.rbac.entity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.app.core.db.BaseAuditEntity;
import com.app.features.user.entity.UserBaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "role", indexes = {
        @Index(name = "idx_role_created_at", columnList = "created_at"),
        @Index(name = "idx_role_updated_at", columnList = "updated_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleEntity extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Column(name = "role_key")
    private String key;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<PermissionEntity> permissions;

    @ManyToMany(mappedBy = "roles")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<UserBaseEntity> users;
}
