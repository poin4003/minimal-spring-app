package com.app.features.post.catalogpost.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import com.app.features.post.catalogpost.entity.CatalogAttributeEntity;

import jakarta.persistence.LockModeType;

public interface CatalogAttributeRepository
        extends JpaRepository<CatalogAttributeEntity, UUID>,
        JpaSpecificationExecutor<CatalogAttributeEntity> {

    Optional<CatalogAttributeEntity> findByKey(String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CatalogAttributeEntity> findForUpdateById(UUID attributeId);

    boolean existsByKey(String key);

}
