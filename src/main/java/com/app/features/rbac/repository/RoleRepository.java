package com.app.features.rbac.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.app.features.rbac.entity.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID>, JpaSpecificationExecutor<RoleEntity> {

    boolean existsByKey(String key);

    Optional<RoleEntity> findByKey(String key);
}
