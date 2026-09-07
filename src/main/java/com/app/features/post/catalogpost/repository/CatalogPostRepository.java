package com.app.features.post.catalogpost.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import com.app.features.post.catalogpost.entity.CatalogPostEntity;
import com.app.features.post.catalogpost.entity.CatalogPostEntity_;
import com.app.features.post.entity.PostEntity_;

import jakarta.persistence.LockModeType;

public interface CatalogPostRepository
        extends JpaRepository<CatalogPostEntity, UUID>,
        JpaSpecificationExecutor<CatalogPostEntity> {

    @Override
    @EntityGraph(attributePaths = {
            CatalogPostEntity_.POST,
            CatalogPostEntity_.POST + "." + PostEntity_.AUTHOR,
            CatalogPostEntity_.CATEGORY
    })
    Page<CatalogPostEntity> findAll(
            Specification<CatalogPostEntity> specification,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            CatalogPostEntity_.POST,
            CatalogPostEntity_.POST + "." + PostEntity_.AUTHOR,
            CatalogPostEntity_.POST + "." + PostEntity_.MODERATED_BY,
            CatalogPostEntity_.CATEGORY
    })
    Optional<CatalogPostEntity> findDetailByPostId(UUID postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            CatalogPostEntity_.POST,
            CatalogPostEntity_.POST + "." + PostEntity_.AUTHOR,
            CatalogPostEntity_.POST + "." + PostEntity_.MODERATED_BY,
            CatalogPostEntity_.CATEGORY
    })
    Optional<CatalogPostEntity> findForUpdateByPostId(UUID postId);

    @EntityGraph(attributePaths = {
            CatalogPostEntity_.POST,
            CatalogPostEntity_.POST + "." + PostEntity_.AUTHOR,
            CatalogPostEntity_.CATEGORY
    })
    List<CatalogPostEntity> findAllByPostIdIn(Collection<UUID> postIds);
}
