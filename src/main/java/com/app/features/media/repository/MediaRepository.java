package com.app.features.media.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.app.core.enums.RecordStatus;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.entity.MediaEntity_;
import com.app.features.media.enums.MediaProcessingStatus;

public interface MediaRepository
        extends JpaRepository<MediaEntity, UUID>, JpaSpecificationExecutor<MediaEntity> {

    @Override
    @EntityGraph(attributePaths = MediaEntity_.CREATED_BY)
    Page<MediaEntity> findAll(Specification<MediaEntity> specification, Pageable pageable);

    Optional<MediaEntity> findByPublicKeyAndStatusAndProcessingStatus(
            String publicKey,
            RecordStatus status,
            MediaProcessingStatus processingStatus);

    Optional<MediaEntity> findByIdAndCreatedBy_Id(UUID mediaId, UUID createdById);

    List<MediaEntity> findAllByIdInAndCreatedBy_IdAndStatusAndProcessingStatus(
            Collection<UUID> mediaIds,
            UUID createdById,
            RecordStatus status,
            MediaProcessingStatus processingStatus);
}
