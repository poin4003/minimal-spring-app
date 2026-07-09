package com.app.features.rbac.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.features.rbac.entity.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID>, JpaSpecificationExecutor<RoleEntity> {

    @Query(value = """
            SELECT r.* FROM role r
            INNER JOIN user_roles ur ON r.id = ur.role_id
            WHERE ur.user_id = :userId
            """, nativeQuery = true)
    List<RoleEntity> findByUserId(@Param("userId") UUID userId);

    boolean existsByKey(String key);

    Optional<RoleEntity> findByKey(String key);
}
