package com.app.features.post.videopost.service.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.service.MediaService;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.videopost.entity.VideoSeriesEntity;
import com.app.features.post.videopost.mapper.VideoSeriesResultMapper;
import com.app.features.post.videopost.repository.VideoSeriesItemRepository;
import com.app.features.post.videopost.repository.VideoSeriesRepository;
import com.app.features.post.videopost.repository.spec.VideoSeriesSpecification;
import com.app.features.post.videopost.schema.filter.VideoSeriesFilterCriteria;
import com.app.features.post.videopost.schema.payload.CreateVideoSeriesPayload;
import com.app.features.post.videopost.schema.payload.UpdateVideoSeriesPayload;
import com.app.features.post.videopost.schema.result.VideoSeriesResult;
import com.app.features.post.videopost.service.VideoSeriesService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.service.ProfileService;
import com.app.features.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VideoSeriesServiceImpl implements VideoSeriesService {

    private final UserService userSvc;
    private final ProfileService profileSvc;
    private final MediaService mediaSvc;
    private final VideoSeriesRepository videoSeriesRepo;
    private final VideoSeriesItemRepository videoSeriesItemRepo;
    private final VideoSeriesResultMapper videoSeriesMapper;

    @Override
    @Transactional
    public VideoSeriesResult createSeries(
            UUID ownerId,
            CreateVideoSeriesPayload payload) {
        UserBaseEntity owner = userSvc.requireUser(ownerId);

        VideoSeriesEntity series = new VideoSeriesEntity();
        series.setOwner(owner);
        series.setCoverMedia(resolveOwnedCover(
                payload.getCoverMediaId(),
                ownerId));
        series.setTitle(payload.getTitle());
        series.setDescription(payload.getDescription());
        series.setVideoCount(0);
        series = videoSeriesRepo.save(series);

        return toResult(series);
    }

    @Override
    @Transactional
    public VideoSeriesResult updateOwnedSeries(
            UUID seriesId,
            UUID ownerId,
            UpdateVideoSeriesPayload payload) {
        VideoSeriesEntity series = requireOwnedSeriesForUpdate(
                seriesId,
                ownerId);

        series.setCoverMedia(resolveOwnedCover(
                payload.getCoverMediaId(),
                ownerId));
        series.setTitle(payload.getTitle());
        series.setDescription(payload.getDescription());

        return toResult(series);
    }

    @Override
    @Transactional
    public void deleteOwnedSeries(
            UUID seriesId,
            UUID ownerId) {
        videoSeriesRepo.delete(requireOwnedSeriesForUpdate(
                seriesId,
                ownerId));
    }

    @Override
    @Transactional
    public void deleteOwnedSeriesWithVideos(
            UUID seriesId,
            UUID ownerId) {
        VideoSeriesEntity series = requireOwnedSeriesForUpdate(
                seriesId,
                ownerId);

        if (videoSeriesItemRepo.countVideoLinksOutsideSeries(seriesId) > 0) {
            throw ExceptionFactory.invalidParam(
                    "error.videoSeries.containsSharedVideos",
                    seriesId);
        }

        videoSeriesItemRepo.softDeleteVideoPostsBySeriesId(
                seriesId,
                ownerId,
                PostLifecycleStatus.DELETED,
                LocalDateTime.now());
        videoSeriesRepo.delete(series);
    }

    @Override
    public VideoSeriesResult getPublishedSeries(UUID seriesId) {
        return toResult(requirePublishedSeries(seriesId));
    }

    @Override
    public VideoSeriesResult getOwnedSeries(
            UUID seriesId,
            UUID ownerId) {
        return toResult(requireOwnedSeries(seriesId, ownerId));
    }

    @Override
    public Page<VideoSeriesResult> getPublishedSeries(
            VideoSeriesFilterCriteria criteria,
            Pageable pageable) {
        return mapSeriesPage(videoSeriesRepo.findAll(
                VideoSeriesSpecification.published(criteria),
                pageable));
    }

    @Override
    public Page<VideoSeriesResult> getOwnedSeries(
            UUID ownerId,
            VideoSeriesFilterCriteria criteria,
            Pageable pageable) {
        return mapSeriesPage(videoSeriesRepo.findAll(
                VideoSeriesSpecification.ownedBy(ownerId, criteria),
                pageable));
    }

    @Override
    public VideoSeriesEntity requireSeries(UUID seriesId) {
        return videoSeriesRepo.findDetailById(seriesId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.videoSeries.notFound",
                        seriesId));
    }

    @Override
    public VideoSeriesEntity requirePublishedSeries(UUID seriesId) {
        VideoSeriesEntity series = requireSeries(seriesId);
        boolean hasPublishedItems = videoSeriesItemRepo
                .existsBySeries_IdAndVideoPost_Post_LifecycleStatusAndVideoPost_Post_ModerationStatus(
                        seriesId,
                        PostLifecycleStatus.ACTIVE,
                        PostModerationStatus.PUBLISHED);

        if (!hasPublishedItems) {
            throw ExceptionFactory.notFound(
                    "error.videoSeries.notFound",
                    seriesId);
        }
        return series;
    }

    @Override
    public VideoSeriesEntity requireOwnedSeries(
            UUID seriesId,
            UUID ownerId) {
        return requireOwnedSeries(requireSeries(seriesId), ownerId);
    }

    @Override
    public VideoSeriesEntity requireOwnedSeriesForUpdate(
            UUID seriesId,
            UUID ownerId) {
        VideoSeriesEntity series = videoSeriesRepo.findForUpdateById(seriesId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.videoSeries.notFound",
                        seriesId));
        return requireOwnedSeries(series, ownerId);
    }

    private VideoSeriesEntity requireOwnedSeries(
            VideoSeriesEntity series,
            UUID ownerId) {
        if (!series.getOwner().getId().equals(ownerId)) {
            throw ExceptionFactory.notFound(
                    "error.videoSeries.notFound",
                    series.getId());
        }
        return series;
    }

    private MediaEntity resolveOwnedCover(
            UUID coverMediaId,
            UUID ownerId) {
        if (coverMediaId == null) {
            return null;
        }

        MediaEntity cover = mediaSvc.requireOwnedActiveMedia(
                coverMediaId,
                ownerId);
        if (cover.getKind() != MediaKind.IMAGE) {
            throw ExceptionFactory.invalidParam(
                    "error.videoSeries.coverKindNotAllowed",
                    cover.getKind());
        }
        return cover;
    }

    private Page<VideoSeriesResult> mapSeriesPage(
            Page<VideoSeriesEntity> entityPage) {
        Map<UUID, UserInfoEntity> profilesByOwnerId = profileSvc
                .requireProfiles(entityPage.getContent().stream()
                        .map(series -> series.getOwner().getId())
                        .toList());

        return entityPage.map(series -> videoSeriesMapper.toResult(
                series,
                profileSvc.requireProfile(
                        profilesByOwnerId,
                        series.getOwner().getId())));
    }

    private VideoSeriesResult toResult(VideoSeriesEntity series) {
        return videoSeriesMapper.toResult(
                series,
                profileSvc.requireProfile(series.getOwner().getId()));
    }
}
