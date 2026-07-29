package com.app.features.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.features.auth.entity.RegistrationEntity;

import jakarta.persistence.LockModeType;

public interface RegistrationRepository
        extends JpaRepository<RegistrationEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RegistrationEntity> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RegistrationEntity> findByCompletionTokenHash(
            String completionTokenHash);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RegistrationEntity> findById(UUID registrationId);

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true)
    @Query("""
            DELETE FROM RegistrationEntity registration
            WHERE registration.updatedAt < :cutoff
            """)
    int deleteStaleRegistrations(
            @Param("cutoff") LocalDateTime cutoff);
}
