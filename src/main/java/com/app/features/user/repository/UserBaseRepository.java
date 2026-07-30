package com.app.features.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.enums.UserStatusEnum;

import jakarta.persistence.LockModeType;

public interface UserBaseRepository extends JpaRepository<UserBaseEntity, UUID> {
    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserBaseEntity> findOneById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserBaseEntity> findOneByEmailAndStatus(
            String email,
            UserStatusEnum status);

    @EntityGraph(attributePaths = { "roles", "roles.permissions" })
    Optional<UserBaseEntity> findByEmail(String email);

    @EntityGraph(attributePaths = { "roles", "roles.permissions" })
    Optional<UserBaseEntity> findWithAuthoritiesById(UUID id);
}
