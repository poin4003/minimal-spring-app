package com.app.features.post.catalogpost.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.app.core.enums.RecordStatus;
import com.app.features.post.catalogpost.entity.CatalogAttributeOptionEntity;
import com.app.features.post.catalogpost.entity.CatalogAttributeOptionEntity_;

import jakarta.persistence.LockModeType;

public interface CatalogAttributeOptionRepository
        extends JpaRepository<CatalogAttributeOptionEntity, UUID> {

    @EntityGraph(attributePaths = CatalogAttributeOptionEntity_.ATTRIBUTE)
    Page<CatalogAttributeOptionEntity> findAllByAttribute_Id(
            UUID attributeId,
            Pageable pageable);

    @EntityGraph(attributePaths = CatalogAttributeOptionEntity_.ATTRIBUTE)
    Optional<CatalogAttributeOptionEntity> findByIdAndAttribute_Id(
            UUID optionId,
            UUID attributeId);

    @EntityGraph(attributePaths = CatalogAttributeOptionEntity_.ATTRIBUTE)
    List<CatalogAttributeOptionEntity>
            findAllByAttribute_IdInOrderByAttribute_IdAscSortOrderAsc(
                    Collection<UUID> attributeIds);

    @EntityGraph(attributePaths = CatalogAttributeOptionEntity_.ATTRIBUTE)
    List<CatalogAttributeOptionEntity>
            findAllByAttribute_IdInAndStatusOrderByAttribute_IdAscSortOrderAsc(
                    Collection<UUID> attributeIds,
                    RecordStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = CatalogAttributeOptionEntity_.ATTRIBUTE)
    Optional<CatalogAttributeOptionEntity> findForUpdateById(
            UUID optionId);

    boolean existsByAttribute_Id(UUID attributeId);

    boolean existsByAttribute_IdAndCode(UUID attributeId, String code);

}
