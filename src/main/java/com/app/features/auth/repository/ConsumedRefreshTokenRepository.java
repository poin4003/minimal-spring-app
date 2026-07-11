package com.app.features.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.features.auth.entity.ConsumedRefreshTokenEntity;

public interface ConsumedRefreshTokenRepository extends JpaRepository<ConsumedRefreshTokenEntity, UUID> {
    boolean existsByTokenValue(String tokenValue);

    @Modifying
    @Query("DELETE FROM ConsumedRefreshTokenEntity c WHERE c.expiryDate < :now")
    int deleteAllExpiredSince(@Param("now") Instant now);

    Optional<ConsumedRefreshTokenEntity> findByTokenValue(String refreshToken);
}
