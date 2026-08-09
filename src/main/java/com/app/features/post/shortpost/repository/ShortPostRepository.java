package com.app.features.post.shortpost.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.app.features.post.entity.PostEntity_;
import com.app.features.post.shortpost.entity.ShortPostEntity;
import com.app.features.post.shortpost.entity.ShortPostEntity_;

public interface ShortPostRepository
        extends JpaRepository<ShortPostEntity, UUID>,
        JpaSpecificationExecutor<ShortPostEntity> {

    @Override
    @EntityGraph(attributePaths = {
            ShortPostEntity_.POST,
            ShortPostEntity_.POST + "." + PostEntity_.AUTHOR
    })
    Page<ShortPostEntity> findAll(
            Specification<ShortPostEntity> specification,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            ShortPostEntity_.POST,
            ShortPostEntity_.POST + "." + PostEntity_.AUTHOR,
            ShortPostEntity_.POST + "." + PostEntity_.MODERATED_BY
    })
    Optional<ShortPostEntity> findDetailByPostId(UUID postId);
}
