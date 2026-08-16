package com.app.features.post.videopost.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.features.post.entity.PostEntity_;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.videopost.entity.VideoPostEntity_;
import com.app.features.post.videopost.entity.VideoSeriesItemEntity;
import com.app.features.post.videopost.entity.VideoSeriesItemEntity_;

import jakarta.persistence.LockModeType;

public interface VideoSeriesItemRepository
        extends JpaRepository<VideoSeriesItemEntity, UUID> {

    @EntityGraph(attributePaths = {
            VideoSeriesItemEntity_.VIDEO_POST,
            VideoSeriesItemEntity_.VIDEO_POST + "." + VideoPostEntity_.POST,
            VideoSeriesItemEntity_.VIDEO_POST + "." + VideoPostEntity_.POST
                    + "." + PostEntity_.AUTHOR
    })
    Page<VideoSeriesItemEntity> findAllBySeries_Id(
            UUID seriesId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            VideoSeriesItemEntity_.VIDEO_POST,
            VideoSeriesItemEntity_.VIDEO_POST + "." + VideoPostEntity_.POST,
            VideoSeriesItemEntity_.VIDEO_POST + "." + VideoPostEntity_.POST
                    + "." + PostEntity_.AUTHOR
    })
    Page<VideoSeriesItemEntity> findAllBySeries_IdAndVideoPost_Post_LifecycleStatusAndVideoPost_Post_ModerationStatus(
            UUID seriesId,
            PostLifecycleStatus lifecycleStatus,
            PostModerationStatus moderationStatus,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            VideoSeriesItemEntity_.VIDEO_POST,
            VideoSeriesItemEntity_.VIDEO_POST + "." + VideoPostEntity_.POST,
            VideoSeriesItemEntity_.VIDEO_POST + "." + VideoPostEntity_.POST
                    + "." + PostEntity_.AUTHOR
    })
    List<VideoSeriesItemEntity> findAllBySeries_IdAndVideoPost_PostIdIn(
            UUID seriesId,
            Collection<UUID> videoPostIds);

    boolean existsBySeries_IdAndVideoPost_Post_LifecycleStatusAndVideoPost_Post_ModerationStatus(
            UUID seriesId,
            PostLifecycleStatus lifecycleStatus,
            PostModerationStatus moderationStatus);

    @Query("""
            SELECT COALESCE(MAX(item.position), -1)
            FROM VideoSeriesItemEntity item
            WHERE item.series.id = :seriesId
            """)
    int findMaxPositionBySeriesId(
            @Param("seriesId") UUID seriesId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT item
            FROM VideoSeriesItemEntity item
            WHERE item.id = :itemId
              AND item.series.id = :seriesId
            """)
    Optional<VideoSeriesItemEntity> findForUpdate(
            @Param("seriesId") UUID seriesId,
            @Param("itemId") UUID itemId);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE VideoSeriesItemEntity item
            SET item.position = item.position + :offset
            WHERE item.series.id = :seriesId
              AND item.position BETWEEN :startPosition AND :endPosition
            """)
    int stagePositions(
            @Param("seriesId") UUID seriesId,
            @Param("startPosition") int startPosition,
            @Param("endPosition") int endPosition,
            @Param("offset") int offset);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE VideoSeriesItemEntity item
            SET item.position = item.position - :normalizationOffset
            WHERE item.series.id = :seriesId
              AND item.position BETWEEN :stagedStart AND :stagedEnd
            """)
    int normalizeStagedPositions(
            @Param("seriesId") UUID seriesId,
            @Param("stagedStart") int stagedStart,
            @Param("stagedEnd") int stagedEnd,
            @Param("normalizationOffset") int normalizationOffset);

    @Query("""
            SELECT COUNT(sharedItem)
            FROM VideoSeriesItemEntity sharedItem
            WHERE sharedItem.series.id <> :seriesId
              AND sharedItem.videoPost.postId IN (
                  SELECT item.videoPost.postId
                  FROM VideoSeriesItemEntity item
                  WHERE item.series.id = :seriesId
              )
            """)
    long countVideoLinksOutsideSeries(
            @Param("seriesId") UUID seriesId);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE PostEntity post
            SET post.lifecycleStatus = :archivedStatus,
                post.archivedAt = :archivedAt,
                post.updatedAt = :archivedAt
            WHERE post.author.id = :ownerId
              AND post.id IN (
                  SELECT item.videoPost.postId
                  FROM VideoSeriesItemEntity item
                  WHERE item.series.id = :seriesId
              )
              AND post.lifecycleStatus = :activeStatus
              AND post.moderationStatus = :publishedStatus
            """)
    int archiveVideoPostsBySeriesId(
            @Param("seriesId") UUID seriesId,
            @Param("ownerId") UUID ownerId,
            @Param("activeStatus") PostLifecycleStatus activeStatus,
            @Param("archivedStatus") PostLifecycleStatus archivedStatus,
            @Param("publishedStatus") PostModerationStatus publishedStatus,
            @Param("archivedAt") LocalDateTime archivedAt);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE PostEntity post
            SET post.lifecycleStatus = :activeStatus,
                post.archivedAt = NULL,
                post.updatedAt = :restoredAt
            WHERE post.author.id = :ownerId
              AND post.id IN (
                  SELECT item.videoPost.postId
                  FROM VideoSeriesItemEntity item
                  WHERE item.series.id = :seriesId
              )
              AND post.lifecycleStatus = :archivedStatus
              AND post.archivedAt = :archivedAt
              AND post.moderationStatus = :publishedStatus
            """)
    int restoreArchivedVideoPostsBySeriesId(
            @Param("seriesId") UUID seriesId,
            @Param("ownerId") UUID ownerId,
            @Param("archivedStatus") PostLifecycleStatus archivedStatus,
            @Param("activeStatus") PostLifecycleStatus activeStatus,
            @Param("publishedStatus") PostModerationStatus publishedStatus,
            @Param("archivedAt") LocalDateTime archivedAt,
            @Param("restoredAt") LocalDateTime restoredAt);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE PostEntity post
            SET post.lifecycleStatus = :lifecycleStatus,
                post.archivedAt = NULL,
                post.deletedAt = :deletedAt,
                post.updatedAt = :deletedAt
            WHERE post.author.id = :ownerId
              AND post.id IN (
                  SELECT item.videoPost.postId
                  FROM VideoSeriesItemEntity item
                  WHERE item.series.id = :seriesId
              )
              AND post.lifecycleStatus <> :lifecycleStatus
            """)
    int softDeleteVideoPostsBySeriesId(
            @Param("seriesId") UUID seriesId,
            @Param("ownerId") UUID ownerId,
            @Param("lifecycleStatus") PostLifecycleStatus lifecycleStatus,
            @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE PostEntity post
            SET post.lifecycleStatus = :draftStatus,
                post.moderationStatus = NULL,
                post.publishedAt = NULL,
                post.moderatedBy = NULL,
                post.moderatedAt = NULL,
                post.rejectionReason = NULL,
                post.archivedAt = NULL,
                post.deletedAt = NULL,
                post.updatedAt = :restoredAt
            WHERE post.author.id = :ownerId
              AND post.id IN (
                  SELECT item.videoPost.postId
                  FROM VideoSeriesItemEntity item
                  WHERE item.series.id = :seriesId
              )
              AND post.lifecycleStatus = :deletedStatus
              AND post.deletedAt = :deletedAt
            """)
    int restoreDeletedVideoPostsBySeriesId(
            @Param("seriesId") UUID seriesId,
            @Param("ownerId") UUID ownerId,
            @Param("deletedStatus") PostLifecycleStatus deletedStatus,
            @Param("draftStatus") PostLifecycleStatus draftStatus,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("restoredAt") LocalDateTime restoredAt);

}
