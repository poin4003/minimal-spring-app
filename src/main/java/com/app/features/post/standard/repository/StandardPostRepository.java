package com.app.features.post.standard.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.app.features.post.entity.PostEntity_;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.post.standard.entity.StandardPostEntity_;

public interface StandardPostRepository
        extends JpaRepository<StandardPostEntity, UUID>, JpaSpecificationExecutor<StandardPostEntity> {

    @Override
    @EntityGraph(attributePaths = {
            StandardPostEntity_.POST,
            StandardPostEntity_.POST + "." + PostEntity_.AUTHOR })
    Page<StandardPostEntity> findAll(Specification<StandardPostEntity> specification, Pageable pageable);

    @EntityGraph(attributePaths = {
            StandardPostEntity_.POST,
            StandardPostEntity_.POST + "." + PostEntity_.AUTHOR,
            StandardPostEntity_.POST + "." + PostEntity_.MODERATED_BY })
    Optional<StandardPostEntity> findDetailByPostId(UUID postId);
}
