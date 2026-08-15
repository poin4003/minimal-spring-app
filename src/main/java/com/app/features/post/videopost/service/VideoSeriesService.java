package com.app.features.post.videopost.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.post.videopost.entity.VideoSeriesEntity;
import com.app.features.post.videopost.enums.VideoSeriesCascadeMode;
import com.app.features.post.videopost.schema.filter.VideoSeriesFilterCriteria;
import com.app.features.post.videopost.schema.payload.CreateVideoSeriesPayload;
import com.app.features.post.videopost.schema.payload.UpdateVideoSeriesPayload;
import com.app.features.post.videopost.schema.result.VideoSeriesResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface VideoSeriesService {

    VideoSeriesResult createSeries(
            @NotNull UUID ownerId,
            @NotNull @Valid CreateVideoSeriesPayload payload);

    VideoSeriesResult updateOwnedSeries(
            @NotNull UUID seriesId,
            @NotNull UUID ownerId,
            @NotNull @Valid UpdateVideoSeriesPayload payload);

    void archiveOwnedSeries(
            @NotNull UUID seriesId,
            @NotNull UUID ownerId,
            @NotNull VideoSeriesCascadeMode cascadeMode);

    void restoreArchivedOwnedSeries(
            @NotNull UUID seriesId,
            @NotNull UUID ownerId,
            @NotNull VideoSeriesCascadeMode cascadeMode);

    void deleteOwnedSeries(
            @NotNull UUID seriesId,
            @NotNull UUID ownerId,
            @NotNull VideoSeriesCascadeMode cascadeMode);

    void restoreDeletedOwnedSeries(
            @NotNull UUID seriesId,
            @NotNull UUID ownerId,
            @NotNull VideoSeriesCascadeMode cascadeMode);

    VideoSeriesResult getPublishedSeries(@NotNull UUID seriesId);

    VideoSeriesResult getOwnedSeries(
            @NotNull UUID seriesId,
            @NotNull UUID ownerId);

    Page<VideoSeriesResult> getPublishedSeries(
            @NotNull VideoSeriesFilterCriteria criteria,
            @NotNull Pageable pageable);

    Page<VideoSeriesResult> getOwnedSeries(
            @NotNull UUID ownerId,
            @NotNull VideoSeriesFilterCriteria criteria,
            @NotNull Pageable pageable);

    VideoSeriesEntity requireSeries(@NotNull UUID seriesId);

    VideoSeriesEntity requirePublishedSeries(@NotNull UUID seriesId);

    VideoSeriesEntity requireOwnedSeries(
            @NotNull UUID seriesId,
            @NotNull UUID ownerId);

    VideoSeriesEntity requireOwnedSeriesForUpdate(
            @NotNull UUID seriesId,
            @NotNull UUID ownerId);
}
