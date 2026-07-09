package com.app.features.rbac.repository;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.app.features.rbac.entity.PermissionEntity;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface PermissionRepository
                extends JpaRepository<PermissionEntity, UUID>, JpaSpecificationExecutor<PermissionEntity> {

        @Query("""
                        SELECT p FROM UserBaseEntity u
                        JOIN u.roles r
                        JOIN r.permissions p
                        WHERE u.id = :userId
                        """)
        Set<PermissionEntity> findAllByUserId(@Param("userId") UUID userId);

        boolean existsByKey(String key);
}
