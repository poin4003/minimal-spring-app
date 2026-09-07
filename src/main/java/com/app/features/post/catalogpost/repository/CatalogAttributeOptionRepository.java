package com.app.features.post.catalogpost.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.post.catalogpost.entity.CatalogAttributeOptionEntity;
import com.app.features.post.catalogpost.entity.CatalogAttributeOptionEntity_;

public interface CatalogAttributeOptionRepository
        extends JpaRepository<CatalogAttributeOptionEntity, UUID> {

    @EntityGraph(attributePaths = CatalogAttributeOptionEntity_.ATTRIBUTE)
    List<CatalogAttributeOptionEntity>
            findAllByAttribute_IdOrderBySortOrderAsc(UUID attributeId);

    @EntityGraph(attributePaths = CatalogAttributeOptionEntity_.ATTRIBUTE)
    List<CatalogAttributeOptionEntity>
            findAllByAttribute_IdInOrderByAttribute_IdAscSortOrderAsc(
                    Collection<UUID> attributeIds);
}
