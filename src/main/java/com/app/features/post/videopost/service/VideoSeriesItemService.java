package com.app.features.post.videopost.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.post.videopost.schema.payload.AddVideoSeriesItemsPayload;
import com.app.features.post.videopost.schema.payload.CreateVideoSeriesPostsPayload;
import com.app.features.post.videopost.schema.payload.MoveVideoSeriesItemPayload;
import com.app.features.post.videopost.schema.result.VideoSeriesItemResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface VideoSeriesItemService {

    void addItems(
            @NotNull UUID seriesId,
            @NotNull UUID ownerId,
            @NotNull @Valid AddVideoSeriesItemsPayload payload);

    void createVideoPosts(
            @NotNull UUID seriesId,
            @NotNull UUID ownerId,
            @NotNull @Valid CreateVideoSeriesPostsPayload payload);

    void removeItem(
            @NotNull UUID seriesId,
            @NotNull UUID itemId,
            @NotNull UUID ownerId);

    void moveItem(
            @NotNull UUID seriesId,
            @NotNull UUID itemId,
            @NotNull UUID ownerId,
            @NotNull @Valid MoveVideoSeriesItemPayload payload);

    Page<VideoSeriesItemResult> getPublishedItems(
            @NotNull UUID seriesId,
            @NotNull Pageable pageable);

    Page<VideoSeriesItemResult> getOwnedItems(
            @NotNull UUID seriesId,
            @NotNull UUID ownerId,
            @NotNull Pageable pageable);
}
