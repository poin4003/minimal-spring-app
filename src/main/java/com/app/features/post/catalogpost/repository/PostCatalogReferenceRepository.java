package com.app.features.post.catalogpost.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.app.features.post.catalogpost.entity.CatalogPostEntity_;
import com.app.features.post.catalogpost.entity.PostCatalogReferenceEntity;
import com.app.features.post.catalogpost.entity.PostCatalogReferenceEntity_;
import com.app.features.post.entity.PostEntity_;

public interface PostCatalogReferenceRepository
        extends JpaRepository<PostCatalogReferenceEntity, UUID>,
        JpaSpecificationExecutor<PostCatalogReferenceEntity> {

    @Override
    @EntityGraph(attributePaths = {
            PostCatalogReferenceEntity_.POST,
            PostCatalogReferenceEntity_.POST + "." + PostEntity_.AUTHOR,
            PostCatalogReferenceEntity_.CATALOG_POST,
            PostCatalogReferenceEntity_.CATALOG_POST + "."
                    + CatalogPostEntity_.POST,
            PostCatalogReferenceEntity_.CATALOG_POST + "."
                    + CatalogPostEntity_.POST + "." + PostEntity_.AUTHOR,
            PostCatalogReferenceEntity_.CATALOG_POST + "."
                    + CatalogPostEntity_.CATEGORY
    })
    Page<PostCatalogReferenceEntity> findAll(
            Specification<PostCatalogReferenceEntity> specification,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            PostCatalogReferenceEntity_.POST,
            PostCatalogReferenceEntity_.CATALOG_POST,
            PostCatalogReferenceEntity_.CATALOG_POST + "."
                    + CatalogPostEntity_.POST,
            PostCatalogReferenceEntity_.CATALOG_POST + "."
                    + CatalogPostEntity_.CATEGORY
    })
    List<PostCatalogReferenceEntity> findAllByPost_IdOrderByPositionAsc(
            UUID postId);

    @EntityGraph(attributePaths = {
            PostCatalogReferenceEntity_.POST,
            PostCatalogReferenceEntity_.CATALOG_POST,
            PostCatalogReferenceEntity_.CATALOG_POST + "."
                    + CatalogPostEntity_.POST,
            PostCatalogReferenceEntity_.CATALOG_POST + "."
                    + CatalogPostEntity_.CATEGORY
    })
    List<PostCatalogReferenceEntity>
            findAllByPost_IdInOrderByPost_IdAscPositionAsc(
                    Collection<UUID> postIds);
}
