package com.app.features.media.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.core.enums.RecordStatus;
import com.app.features.media.entity.MediaVariantEntity;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.enums.MediaVariantType;

public interface MediaVariantRepository extends JpaRepository<MediaVariantEntity, UUID> {

    Optional<MediaVariantEntity> findByMedia_IdAndVariantTypeAndVariantKey(
            UUID mediaId,
            MediaVariantType variantType,
            String variantKey);

    Optional<MediaVariantEntity>
            findByMedia_PublicKeyAndMedia_StatusAndMedia_ProcessingStatusAndVariantTypeAndVariantKey(
            String publicKey,
            RecordStatus status,
            MediaProcessingStatus processingStatus,
            MediaVariantType variantType,
            String variantKey);
}
