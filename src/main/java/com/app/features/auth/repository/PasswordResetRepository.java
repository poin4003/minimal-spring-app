package com.app.features.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT passwordReset.user.id
            FROM PasswordResetEntity passwordReset
            WHERE passwordReset.resetTokenHash = :resetTokenHash
            """)
    Optional<UUID> findUserIdByResetTokenHash(
            @Param("resetTokenHash") String resetTokenHash);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetEntity> findById(UUID passwordResetId);

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true)
    @Query("""
            DELETE FROM PasswordResetEntity passwordReset
            WHERE passwordReset.updatedAt < :cutoff
            """)
    int deleteStalePasswordResets(
            @Param("cutoff") LocalDateTime cutoff);
}
