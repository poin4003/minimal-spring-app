package com.app.features.post.videopost.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import com.app.features.post.videopost.schema.payload.MoveVideoSeriesItemPayload;
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
        VideoSeriesItemEntity item = videoSeriesItemRepo
                .findForUpdate(seriesId, itemId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.videoSeries.itemNotFound",
                        itemId));
        int removedPosition = item.getPosition();
        int maxPosition = videoSeriesItemRepo
                .findMaxPositionBySeriesId(seriesId);

        videoSeriesItemRepo.delete(item);
        videoSeriesItemRepo.flush();

        if (removedPosition < maxPosition) {
            shiftPositions(
                    seriesId,
                    removedPosition + 1,
                    maxPosition,
                    maxPosition,
                    -1);
        }

        series.setVideoCount(series.getVideoCount() - 1);
    }

    @Override
    @Transactional
    public void moveItem(
            UUID seriesId,
            UUID itemId,
            UUID ownerId,
            MoveVideoSeriesItemPayload payload) {
        videoSeriesSvc.requireOwnedSeriesForUpdate(seriesId, ownerId);
        VideoSeriesItemEntity item = videoSeriesItemRepo
                .findForUpdate(seriesId, itemId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.videoSeries.itemNotFound",
                        itemId));
        int maxPosition = videoSeriesItemRepo
                .findMaxPositionBySeriesId(seriesId);
        int currentPosition = item.getPosition();
        int targetPosition = payload.getTargetPosition();

        if (targetPosition > maxPosition) {
            throw ExceptionFactory.invalidParam(
                    "error.videoSeries.positionOutOfRange",
                    targetPosition);
        }

        if (currentPosition == targetPosition) {
            return;
        }

        int stagingOffset = maxPosition + 1;
        int temporaryPosition = maxPosition + stagingOffset + 1;
        item.setPosition(temporaryPosition);
        videoSeriesItemRepo.saveAndFlush(item);

        if (currentPosition < targetPosition) {
            shiftPositions(
                    seriesId,
                    currentPosition + 1,
                    targetPosition,
                    maxPosition,
                    -1);
        } else {
            shiftPositions(
                    seriesId,
                    targetPosition,
                    currentPosition - 1,
                    maxPosition,
                    1);
        }

        item.setPosition(targetPosition);
    }

    @Override
    public Page<VideoSeriesItemResult> getPublishedItems(
            UUID seriesId,
            Pageable pageable) {
        videoSeriesSvc.requirePublishedSeries(seriesId);
        Page<VideoSeriesItemEntity> entityPage = videoSeriesItemRepo
                .findAllBySeries_IdAndVideoPost_Post_LifecycleStatusAndVideoPost_Post_ModerationStatus(
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
                .findAllBySeries_Id(
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

    private void shiftPositions(
            UUID seriesId,
            int startPosition,
            int endPosition,
            int maxPosition,
            int positionDelta) {
        int stagingOffset = maxPosition + 1;
        videoSeriesItemRepo.stagePositions(
                seriesId,
                startPosition,
                endPosition,
                stagingOffset);
        videoSeriesItemRepo.normalizeStagedPositions(
                seriesId,
                startPosition + stagingOffset,
                endPosition + stagingOffset,
                stagingOffset - positionDelta);
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
