package com.app.features.post.catalogpost.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.app.features.post.catalogpost.entity.CatalogAttributeEntity;

public interface CatalogAttributeRepository
        extends JpaRepository<CatalogAttributeEntity, UUID>,
        JpaSpecificationExecutor<CatalogAttributeEntity> {

    Optional<CatalogAttributeEntity> findByKey(String key);

    boolean existsByKey(String key);
}
