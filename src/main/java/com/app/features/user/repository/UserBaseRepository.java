package com.app.features.user.repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.features.user.entity.UserBaseEntity;

public interface UserBaseRepository extends JpaRepository<UserBaseEntity, UUID> {
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = { "roles", "roles.permissions" })
    Optional<UserBaseEntity> findByEmail(String email);

    @EntityGraph(attributePaths = { "roles", "roles.permissions" })
    Optional<UserBaseEntity> findWithAuthoritiesById(UUID id);

    @Query("""
                SELECT DISTINCT user.id
                FROM UserBaseEntity user
                JOIN user.roles role
                WHERE role.id = :roleId
            """)
    Set<UUID> findAllIdsByRoleId(@Param("roleId") UUID roleId);
}
