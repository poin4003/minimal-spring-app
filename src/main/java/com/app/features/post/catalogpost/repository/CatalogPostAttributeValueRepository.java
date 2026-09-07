package com.app.features.post.catalogpost.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.post.catalogpost.entity.CatalogPostAttributeValueEntity;
import com.app.features.post.catalogpost.entity.CatalogPostAttributeValueEntity_;

public interface CatalogPostAttributeValueRepository
        extends JpaRepository<CatalogPostAttributeValueEntity, UUID> {

    @EntityGraph(attributePaths = {
            CatalogPostAttributeValueEntity_.CATALOG_POST,
            CatalogPostAttributeValueEntity_.ATTRIBUTE,
            CatalogPostAttributeValueEntity_.OPTION
    })
    List<CatalogPostAttributeValueEntity>
            findAllByCatalogPost_PostIdOrderByPositionAsc(UUID postId);

    @EntityGraph(attributePaths = {
            CatalogPostAttributeValueEntity_.CATALOG_POST,
            CatalogPostAttributeValueEntity_.ATTRIBUTE,
            CatalogPostAttributeValueEntity_.OPTION
    })
    List<CatalogPostAttributeValueEntity>
            findAllByCatalogPost_PostIdInOrderByCatalogPost_PostIdAscPositionAsc(
                    Collection<UUID> postIds);
}
