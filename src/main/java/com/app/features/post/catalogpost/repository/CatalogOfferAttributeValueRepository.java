package com.app.features.post.catalogpost.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.post.catalogpost.entity.CatalogOfferAttributeValueEntity;
import com.app.features.post.catalogpost.entity.CatalogOfferAttributeValueEntity_;

public interface CatalogOfferAttributeValueRepository
        extends JpaRepository<CatalogOfferAttributeValueEntity, UUID> {

    @EntityGraph(attributePaths = {
            CatalogOfferAttributeValueEntity_.CATALOG_OFFER,
            CatalogOfferAttributeValueEntity_.ATTRIBUTE,
            CatalogOfferAttributeValueEntity_.OPTION
    })
    List<CatalogOfferAttributeValueEntity>
            findAllByCatalogOffer_IdOrderByPositionAsc(UUID offerId);

    @EntityGraph(attributePaths = {
            CatalogOfferAttributeValueEntity_.CATALOG_OFFER,
            CatalogOfferAttributeValueEntity_.ATTRIBUTE,
            CatalogOfferAttributeValueEntity_.OPTION
    })
    List<CatalogOfferAttributeValueEntity>
            findAllByCatalogOffer_IdInOrderByCatalogOffer_IdAscPositionAsc(
                    Collection<UUID> offerIds);

    boolean existsByAttribute_Id(UUID attributeId);
}
