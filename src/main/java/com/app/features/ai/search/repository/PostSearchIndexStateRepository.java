package com.app.features.ai.search.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.features.ai.search.entity.PostSearchIndexStateEntity;
import com.app.features.ai.search.enums.PostSearchIndexStatus;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.PostModerationStatus;

import jakarta.persistence.LockModeType;

public interface PostSearchIndexStateRepository
        extends JpaRepository<PostSearchIndexStateEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PostSearchIndexStateEntity> findForUpdateByPostId(UUID postId);

    @Query("""
            SELECT state.postId
            FROM PostSearchIndexStateEntity state
            WHERE state.status = :pendingStatus
               OR (
                    state.status = :failedStatus
                    AND (
                        state.nextAttemptAt IS NULL
                        OR state.nextAttemptAt <= :now
                    )
               )
               OR (
                    state.status IN :leasedStatuses
                    AND (
                        state.leaseExpiresAt IS NULL
                        OR state.leaseExpiresAt <= :now
                    )
               )
               OR (
                    state.status = :syncedStatus
                    AND (
                        state.indexedGeneration IS NULL
                        OR state.indexedGeneration <> :indexGeneration
                    )
               )
            ORDER BY state.updatedAt, state.postId
            """)
    List<UUID> findRecoveryCandidateIds(
            @Param("pendingStatus") PostSearchIndexStatus pendingStatus,
            @Param("failedStatus") PostSearchIndexStatus failedStatus,
            @Param("leasedStatuses") Collection<PostSearchIndexStatus> leasedStatuses,
            @Param("syncedStatus") PostSearchIndexStatus syncedStatus,
            @Param("now") LocalDateTime now,
            @Param("indexGeneration") UUID indexGeneration,
            Pageable pageable);

    @Query("""
            SELECT post.id
            FROM PostEntity post
            WHERE post.lifecycleStatus = :lifecycleStatus
              AND post.moderationStatus = :moderationStatus
              AND post.type IN :postTypes
              AND NOT EXISTS (
                  SELECT state.postId
                  FROM PostSearchIndexStateEntity state
                  WHERE state.postId = post.id
              )
            ORDER BY post.updatedAt, post.id
            """)
    List<UUID> findBackfillPostIds(
            @Param("lifecycleStatus") PostLifecycleStatus lifecycleStatus,
            @Param("moderationStatus") PostModerationStatus moderationStatus,
            @Param("postTypes") Collection<PostType> postTypes,
            Pageable pageable);
}
