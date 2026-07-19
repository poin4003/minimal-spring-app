package com.app.features.media.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.app.features.media.entity.MediaProcessingLeaseEntity;

import jakarta.persistence.LockModeType;

public interface MediaProcessingLeaseRepository
        extends JpaRepository<MediaProcessingLeaseEntity, UUID> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MediaProcessingLeaseEntity> findById(UUID mediaId);
}
