package com.app.features.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

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
}
