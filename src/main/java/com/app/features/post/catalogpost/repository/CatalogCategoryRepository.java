package com.app.features.post.catalogpost.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import com.app.features.post.catalogpost.entity.CatalogCategoryEntity;
import com.app.features.post.catalogpost.entity.CatalogCategoryEntity_;

import jakarta.persistence.LockModeType;

public interface CatalogCategoryRepository
        extends JpaRepository<CatalogCategoryEntity, UUID>,
        JpaSpecificationExecutor<CatalogCategoryEntity> {

    @Override
    @EntityGraph(attributePaths = CatalogCategoryEntity_.PARENT)
    Page<CatalogCategoryEntity> findAll(
            Specification<CatalogCategoryEntity> specification,
            Pageable pageable);

    @EntityGraph(attributePaths = CatalogCategoryEntity_.PARENT)
    Optional<CatalogCategoryEntity> findDetailById(UUID categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = CatalogCategoryEntity_.PARENT)
    Optional<CatalogCategoryEntity> findForUpdateById(UUID categoryId);

    Optional<CatalogCategoryEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID categoryId);

}
