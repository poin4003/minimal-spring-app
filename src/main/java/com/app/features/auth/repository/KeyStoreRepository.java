package com.app.features.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import com.app.features.auth.entity.KeyStoreEntity;

@Repository
public interface KeyStoreRepository extends JpaRepository<KeyStoreEntity, UUID> {
    Optional<KeyStoreEntity> findByUserId(UUID userId);

    @Modifying
    void deleteByUserId(UUID userId);
}
