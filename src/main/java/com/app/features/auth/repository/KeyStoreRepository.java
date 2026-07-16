package com.app.features.auth.repository;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.features.auth.entity.KeyStoreEntity;

public interface KeyStoreRepository extends JpaRepository<KeyStoreEntity, UUID> {

    @Modifying
    @Query("DELETE FROM KeyStoreEntity keyStore WHERE keyStore.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM KeyStoreEntity keyStore WHERE keyStore.userId IN :userIds")
    int deleteAllByUserIds(@Param("userIds") Collection<UUID> userIds);
}
