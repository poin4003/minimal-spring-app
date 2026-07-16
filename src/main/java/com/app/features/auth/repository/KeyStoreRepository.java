package com.app.features.auth.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.auth.entity.KeyStoreEntity;

public interface KeyStoreRepository extends JpaRepository<KeyStoreEntity, UUID> {
}
