package com.app.features.rbac.repository;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.app.features.rbac.entity.PermissionEntity;

public interface PermissionRepository
                extends JpaRepository<PermissionEntity, UUID>, JpaSpecificationExecutor<PermissionEntity> {

        Set<PermissionEntity> findDistinctByRoles_Users_Id(UUID userId);

        boolean existsByKey(String key);
}
