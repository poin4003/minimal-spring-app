package com.app.features.media.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.app.features.media.entity.MediaUploadSessionEntity;
import com.app.features.media.entity.MediaUploadSessionEntity_;

import jakarta.persistence.LockModeType;

public interface MediaUploadSessionRepository
        extends JpaRepository<MediaUploadSessionEntity, UUID> {

    Page<MediaUploadSessionEntity> findAllByExpiresAtBefore(
            LocalDateTime cutoff,
            Pageable pageable);

    @EntityGraph(attributePaths = MediaUploadSessionEntity_.COMPLETED_MEDIA)
    Optional<MediaUploadSessionEntity> findByIdAndCreatedBy_Id(
            UUID uploadId,
            UUID createdById);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = MediaUploadSessionEntity_.COMPLETED_MEDIA)
    Optional<MediaUploadSessionEntity> findOneByIdAndCreatedBy_Id(
            UUID uploadId,
            UUID createdById);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MediaUploadSessionEntity> findOneById(UUID uploadId);
}
