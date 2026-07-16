package com.app.features.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.user.entity.UserBaseEntity;

public interface UserBaseRepository extends JpaRepository<UserBaseEntity, UUID> {
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = { "roles", "roles.permissions" })
    Optional<UserBaseEntity> findByEmail(String email);

    @EntityGraph(attributePaths = { "roles", "roles.permissions" })
    Optional<UserBaseEntity> findWithAuthoritiesById(UUID id);
}
