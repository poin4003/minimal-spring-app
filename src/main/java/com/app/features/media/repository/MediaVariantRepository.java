package com.app.features.media.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.core.enums.RecordStatus;
import com.app.features.media.entity.MediaVariantEntity;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.enums.MediaVariantType;

public interface MediaVariantRepository extends JpaRepository<MediaVariantEntity, UUID> {

    List<MediaVariantEntity> findAllByMedia_IdOrderByVariantTypeAscHeightAsc(UUID mediaId);

    void deleteAllByMedia_Id(UUID mediaId);

    Optional<MediaVariantEntity>
            findByMedia_PublicKeyAndMedia_StatusAndMedia_ProcessingStatusAndVariantTypeAndVariantKey(
            String publicKey,
            RecordStatus status,
            MediaProcessingStatus processingStatus,
            MediaVariantType variantType,
            String variantKey);
}
