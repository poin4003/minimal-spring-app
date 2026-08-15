package com.app.features.post.videopost.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import com.app.features.post.videopost.entity.VideoSeriesEntity;
import com.app.features.post.videopost.entity.VideoSeriesEntity_;

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
}
