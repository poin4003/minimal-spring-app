package com.app.features.media.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.media.entity.MediaVariantEntity;
import com.app.features.media.enums.MediaVariantType;

public interface MediaVariantRepository extends JpaRepository<MediaVariantEntity, UUID> {

    Optional<MediaVariantEntity> findByMedia_IdAndVariantType(
            UUID mediaId,
            MediaVariantType variantType);
}
