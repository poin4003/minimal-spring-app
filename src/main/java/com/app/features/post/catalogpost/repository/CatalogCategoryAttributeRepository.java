package com.app.features.post.catalogpost.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.core.enums.RecordStatus;
import com.app.features.post.catalogpost.entity.CatalogCategoryAttributeEntity;
import com.app.features.post.catalogpost.entity.CatalogCategoryAttributeEntity_;

public interface CatalogCategoryAttributeRepository
        extends JpaRepository<CatalogCategoryAttributeEntity, UUID> {

    @EntityGraph(attributePaths = {
            CatalogCategoryAttributeEntity_.CATEGORY,
            CatalogCategoryAttributeEntity_.ATTRIBUTE
    })
    List<CatalogCategoryAttributeEntity>
            findAllByCategory_IdOrderBySortOrderAsc(UUID categoryId);

    @EntityGraph(attributePaths = {
            CatalogCategoryAttributeEntity_.CATEGORY,
            CatalogCategoryAttributeEntity_.ATTRIBUTE
    })
    List<CatalogCategoryAttributeEntity>
            findAllByCategory_IdAndAttribute_StatusOrderBySortOrderAsc(
                    UUID categoryId,
                    RecordStatus status);

    @EntityGraph(attributePaths = {
            CatalogCategoryAttributeEntity_.CATEGORY,
            CatalogCategoryAttributeEntity_.ATTRIBUTE
    })
    List<CatalogCategoryAttributeEntity>
            findAllByCategory_IdInOrderByCategory_IdAscSortOrderAsc(
                    Collection<UUID> categoryIds);

    boolean existsByCategory_IdAndAttribute_Id(
            UUID categoryId,
            UUID attributeId);

    boolean existsByAttribute_Id(UUID attributeId);
}
