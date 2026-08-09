package com.app.features.post.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostEntity_;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;

import jakarta.persistence.LockModeType;

public interface PostRepository extends JpaRepository<PostEntity, UUID>, JpaSpecificationExecutor<PostEntity> {

    @Override
    @EntityGraph(attributePaths = PostEntity_.AUTHOR)
    Page<PostEntity> findAll(Specification<PostEntity> specification, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            PostEntity_.AUTHOR,
            PostEntity_.MODERATED_BY })
    Optional<PostEntity> findForUpdateById(UUID postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM PostEntity post
            WHERE post.lifecycleStatus = :lifecycleStatus
              AND post.deletedAt < :cutoff
            """)
    int deleteExpiredDeletedPosts(
            @Param("lifecycleStatus") PostLifecycleStatus lifecycleStatus,
            @Param("cutoff") LocalDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM PostEntity post
            WHERE post.lifecycleStatus = :lifecycleStatus
              AND post.moderationStatus = :moderationStatus
              AND post.moderatedAt < :cutoff
            """)
    int deleteExpiredRejectedPosts(
            @Param("lifecycleStatus") PostLifecycleStatus lifecycleStatus,
            @Param("moderationStatus") PostModerationStatus moderationStatus,
            @Param("cutoff") LocalDateTime cutoff);
}
