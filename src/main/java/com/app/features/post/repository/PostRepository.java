package com.app.features.post.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostEntity_;

import jakarta.persistence.LockModeType;

public interface PostRepository extends JpaRepository<PostEntity, UUID>, JpaSpecificationExecutor<PostEntity> {

    @Override
    @EntityGraph(attributePaths = PostEntity_.AUTHOR)
    Page<PostEntity> findAll(Specification<PostEntity> specification, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PostEntity> findForUpdateById(UUID postId);
}
