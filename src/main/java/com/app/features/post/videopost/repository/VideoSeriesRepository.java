package com.app.features.post.videopost.repository;

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

import com.app.features.post.videopost.entity.VideoSeriesEntity;
import com.app.features.post.videopost.entity.VideoSeriesEntity_;
import com.app.features.post.videopost.enums.VideoSeriesLifecycleStatus;

import jakarta.persistence.LockModeType;

public interface VideoSeriesRepository
        extends JpaRepository<VideoSeriesEntity, UUID>,
        JpaSpecificationExecutor<VideoSeriesEntity> {

    @Override
    @EntityGraph(attributePaths = {
            VideoSeriesEntity_.OWNER,
            VideoSeriesEntity_.COVER_MEDIA
    })
    Page<VideoSeriesEntity> findAll(
            Specification<VideoSeriesEntity> specification,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            VideoSeriesEntity_.OWNER,
            VideoSeriesEntity_.COVER_MEDIA
    })
    Optional<VideoSeriesEntity> findDetailById(UUID seriesId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            VideoSeriesEntity_.OWNER,
            VideoSeriesEntity_.COVER_MEDIA
    })
    Optional<VideoSeriesEntity> findForUpdateById(UUID seriesId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM VideoSeriesEntity series
            WHERE series.lifecycleStatus = :lifecycleStatus
              AND series.deletedAt < :cutoff
            """)
    int deleteExpiredDeletedSeries(
            @Param("lifecycleStatus")
            VideoSeriesLifecycleStatus lifecycleStatus,
            @Param("cutoff") LocalDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE video_series
            SET video_count = (
                SELECT COUNT(*)
                FROM video_series_item item
                WHERE item.series_id = video_series.id
            )
            WHERE video_count <> (
                SELECT COUNT(*)
                FROM video_series_item item
                WHERE item.series_id = video_series.id
            )
            """, nativeQuery = true)
    int synchronizeVideoCounts();
}
