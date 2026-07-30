package com.app.features.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.app.features.auth.entity.PasswordResetEntity;

import jakarta.persistence.LockModeType;

public interface PasswordResetRepository
        extends JpaRepository<PasswordResetEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetEntity> findByUser_Id(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetEntity> findByUser_Email(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetEntity> findByResetTokenHash(
            String resetTokenHash);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetEntity> findById(UUID passwordResetId);
}
