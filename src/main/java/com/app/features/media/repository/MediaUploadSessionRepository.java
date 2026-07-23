package com.app.features.media.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.features.media.entity.MediaUploadSessionEntity;
import com.app.features.media.entity.MediaUploadSessionEntity_;
import com.app.features.media.enums.MediaUploadStatus;

import jakarta.persistence.LockModeType;

public interface MediaUploadSessionRepository
        extends JpaRepository<MediaUploadSessionEntity, UUID> {

    Page<MediaUploadSessionEntity> findAllByExpiresAtBefore(
            LocalDateTime cutoff,
            Pageable pageable);

    long countByCreatedBy_IdAndStatusIn(
            UUID createdById,
            Collection<MediaUploadStatus> statuses);

    @Query("""
            select coalesce(sum(upload.fileSize), 0)
            from MediaUploadSessionEntity upload
            where upload.createdBy.id = :createdById
              and upload.status in :statuses
            """)
    long sumFileSizeByCreatedByIdAndStatuses(
            @Param("createdById") UUID createdById,
            @Param("statuses") Collection<MediaUploadStatus> statuses);

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
