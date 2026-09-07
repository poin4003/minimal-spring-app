package com.app.features.post.catalogpost.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.post.catalogpost.entity.CatalogOfferEntity;
import com.app.features.post.catalogpost.entity.CatalogOfferEntity_;

public interface CatalogOfferRepository
        extends JpaRepository<CatalogOfferEntity, UUID> {

    @EntityGraph(attributePaths = CatalogOfferEntity_.CATALOG_POST)
    List<CatalogOfferEntity>
            findAllByCatalogPost_PostIdOrderByPositionAsc(UUID postId);

    @EntityGraph(attributePaths = CatalogOfferEntity_.CATALOG_POST)
    List<CatalogOfferEntity>
            findAllByCatalogPost_PostIdInOrderByCatalogPost_PostIdAscPositionAsc(
                    Collection<UUID> postIds);
}
