package com.app.features.post.videopost.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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
    Page<VideoSeriesItemEntity> findAllBySeries_IdOrderByPositionAsc(
            UUID seriesId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            VideoSeriesItemEntity_.VIDEO_POST,
            VideoSeriesItemEntity_.VIDEO_POST + "." + VideoPostEntity_.POST,
            VideoSeriesItemEntity_.VIDEO_POST + "." + VideoPostEntity_.POST
                    + "." + PostEntity_.AUTHOR
    })
    Page<VideoSeriesItemEntity> findAllBySeries_IdAndVideoPost_Post_LifecycleStatusAndVideoPost_Post_ModerationStatusOrderByPositionAsc(
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

    long deleteByIdAndSeries_Id(
            UUID itemId,
            UUID seriesId);

    @Query("""
            SELECT COALESCE(MAX(item.position), -1)
            FROM VideoSeriesItemEntity item
            WHERE item.series.id = :seriesId
            """)
    int findMaxPositionBySeriesId(
            @Param("seriesId") UUID seriesId);

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
            SET post.lifecycleStatus = :lifecycleStatus,
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = VideoSeriesItemEntity_.VIDEO_POST)
    @Query("""
            SELECT item
            FROM VideoSeriesItemEntity item
            WHERE item.series.id = :seriesId
            ORDER BY item.position ASC
            """)
    List<VideoSeriesItemEntity> findAllForUpdateBySeriesId(
            @Param("seriesId") UUID seriesId);
}
