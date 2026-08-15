package com.app.features.post.videopost.service.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.videopost.entity.VideoPostEntity;
import com.app.features.post.videopost.entity.VideoSeriesEntity;
import com.app.features.post.videopost.entity.VideoSeriesItemEntity;
import com.app.features.post.videopost.mapper.VideoSeriesResultMapper;
import com.app.features.post.videopost.repository.VideoSeriesItemRepository;
import com.app.features.post.videopost.schema.payload.AddVideoSeriesItemsPayload;
import com.app.features.post.videopost.schema.payload.CreateVideoSeriesPostsPayload;
import com.app.features.post.videopost.schema.payload.ReorderVideoSeriesItemsPayload;
import com.app.features.post.videopost.schema.result.VideoSeriesItemResult;
import com.app.features.post.videopost.service.VideoPostService;
import com.app.features.post.videopost.service.VideoSeriesItemService;
import com.app.features.post.videopost.service.VideoSeriesService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VideoSeriesItemServiceImpl implements VideoSeriesItemService {

    private final VideoPostService videoPostSvc;
    private final VideoSeriesService videoSeriesSvc;
    private final VideoSeriesItemRepository videoSeriesItemRepo;
    private final VideoSeriesResultMapper videoSeriesMapper;

    @Override
    @Transactional
    public void addItems(
            UUID seriesId,
            UUID ownerId,
            AddVideoSeriesItemsPayload payload) {
        VideoSeriesEntity series = videoSeriesSvc
                .requireOwnedSeriesForUpdate(seriesId, ownerId);
        List<VideoPostEntity> videoPosts = videoPostSvc
                .requireOwnedVideoPosts(
                        payload.getVideoPostIds(),
                        ownerId);
        requireLinkableVideoPosts(videoPosts);

        if (!videoSeriesItemRepo
                .findAllBySeries_IdAndVideoPost_PostIdIn(
                        seriesId,
                        payload.getVideoPostIds())
                .isEmpty()) {
            throw ExceptionFactory.alreadyExists(
                    "error.videoSeries.videoAlreadyAdded",
                    seriesId);
        }

        appendItems(series, videoPosts);
    }

    @Override
    @Transactional
    public void createVideoPosts(
            UUID seriesId,
            UUID ownerId,
            CreateVideoSeriesPostsPayload payload) {
        VideoSeriesEntity series = videoSeriesSvc
                .requireOwnedSeriesForUpdate(seriesId, ownerId);
        List<VideoPostEntity> videoPosts = payload.getVideoPosts()
                .stream()
                .map(videoPayload -> videoPostSvc.createDraftVideoPost(
                        series.getOwner(),
                        videoPayload))
                .toList();

        appendItems(series, videoPosts);
    }

    @Override
    @Transactional
    public void removeItem(
            UUID seriesId,
            UUID itemId,
            UUID ownerId) {
        VideoSeriesEntity series = videoSeriesSvc
                .requireOwnedSeriesForUpdate(seriesId, ownerId);
        long deletedCount = videoSeriesItemRepo.deleteByIdAndSeries_Id(
                itemId,
                seriesId);

        if (deletedCount == 0) {
            throw ExceptionFactory.notFound(
                    "error.videoSeries.itemNotFound",
                    itemId);
        }

        series.setVideoCount(series.getVideoCount() - 1);
    }

    @Override
    @Transactional
    public void reorderItems(
            UUID seriesId,
            UUID ownerId,
            ReorderVideoSeriesItemsPayload payload) {
        videoSeriesSvc.requireOwnedSeriesForUpdate(seriesId, ownerId);
        List<VideoSeriesItemEntity> currentItems = videoSeriesItemRepo
                .findAllForUpdateBySeriesId(seriesId);
        List<UUID> orderedItemIds = payload.getSeriesItemIds();
        Set<UUID> distinctItemIds = new LinkedHashSet<>(orderedItemIds);
        Set<UUID> currentItemIds = currentItems.stream()
                .map(item -> item.getId())
                .collect(Collectors.toSet());

        if (orderedItemIds.size() != currentItems.size()
                || distinctItemIds.size() != currentItems.size()
                || !currentItemIds.equals(distinctItemIds)) {
            throw ExceptionFactory.invalidParam(
                    "error.videoSeries.reorderMismatch",
                    seriesId);
        }

        if (currentItems.isEmpty()) {
            return;
        }

        Map<UUID, VideoSeriesItemEntity> itemById = currentItems.stream()
                .collect(Collectors.toMap(
                        item -> item.getId(),
                        item -> item));
        int temporaryPositionStart = currentItems.stream()
                .mapToInt(item -> item.getPosition())
                .max()
                .orElse(-1) + 1;

        IntStream.range(0, currentItems.size())
                .forEach(index -> currentItems.get(index).setPosition(
                        temporaryPositionStart + index));
        videoSeriesItemRepo.saveAllAndFlush(currentItems);

        List<VideoSeriesItemEntity> reorderedItems = IntStream
                .range(0, orderedItemIds.size())
                .mapToObj(position -> {
                    VideoSeriesItemEntity item = itemById.get(
                            orderedItemIds.get(position));
                    item.setPosition(position);
                    return item;
                })
                .toList();
        videoSeriesItemRepo.saveAll(reorderedItems);
    }

    @Override
    public Page<VideoSeriesItemResult> getPublishedItems(
            UUID seriesId,
            Pageable pageable) {
        videoSeriesSvc.requirePublishedSeries(seriesId);
        Page<VideoSeriesItemEntity> entityPage = videoSeriesItemRepo
                .findAllBySeries_IdAndVideoPost_Post_LifecycleStatusAndVideoPost_Post_ModerationStatusOrderByPositionAsc(
                        seriesId,
                        PostLifecycleStatus.ACTIVE,
                        PostModerationStatus.PUBLISHED,
                        pageable);
        return mapItemPage(entityPage);
    }

    @Override
    public Page<VideoSeriesItemResult> getOwnedItems(
            UUID seriesId,
            UUID ownerId,
            Pageable pageable) {
        videoSeriesSvc.requireOwnedSeries(seriesId, ownerId);
        return mapItemPage(videoSeriesItemRepo
                .findAllBySeries_IdOrderByPositionAsc(
                        seriesId,
                        pageable));
    }

    private void requireLinkableVideoPosts(
            List<VideoPostEntity> videoPosts) {
        if (videoPosts.stream().anyMatch(videoPost -> videoPost
                .getPost()
                .getLifecycleStatus() == PostLifecycleStatus.DELETED)) {
            throw ExceptionFactory.invalidParam(
                    "error.videoSeries.videoSelectionInvalid");
        }
    }

    private void appendItems(
            VideoSeriesEntity series,
            List<VideoPostEntity> videoPosts) {
        int initialPosition = videoSeriesItemRepo
                .findMaxPositionBySeriesId(series.getId()) + 1;
        List<VideoSeriesItemEntity> items = IntStream
                .range(0, videoPosts.size())
                .mapToObj(index -> {
                    VideoSeriesItemEntity item = new VideoSeriesItemEntity();
                    item.setSeries(series);
                    item.setVideoPost(videoPosts.get(index));
                    item.setPosition(initialPosition + index);
                    return item;
                })
                .toList();

        videoSeriesItemRepo.saveAll(items);
        series.setVideoCount(series.getVideoCount() + items.size());
    }

    private Page<VideoSeriesItemResult> mapItemPage(
            Page<VideoSeriesItemEntity> entityPage) {
        List<UUID> postIds = entityPage.getContent().stream()
                .map(item -> item.getVideoPost().getPostId())
                .toList();
        Map<UUID, PostMediaEntity> contentByPostId = videoPostSvc
                .requireContentAttachments(postIds);

        return entityPage.map(item -> videoSeriesMapper.toItemResult(
                item,
                contentByPostId.get(item.getVideoPost().getPostId())));
    }
}
