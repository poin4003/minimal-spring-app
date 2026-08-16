package com.app.features.post.videopost.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.post.entity.PostEntity_;
import com.app.features.post.videopost.entity.VideoPostEntity_;
import com.app.features.post.videopost.entity.VideoSeriesEntity_;
import com.app.features.post.videopost.entity.VideoSeriesItemEntity_;
import com.app.features.post.videopost.schema.filter.PublicVideoPostFilterCriteria;
import com.app.features.post.videopost.schema.filter.VideoSeriesFilterCriteria;
import com.app.features.post.videopost.schema.result.PublicVideoPostResult;
import com.app.features.post.videopost.schema.result.VideoSeriesItemResult;
import com.app.features.post.videopost.schema.result.VideoSeriesResult;
import com.app.features.post.videopost.service.VideoPostService;
import com.app.features.post.videopost.service.VideoSeriesItemService;
import com.app.features.post.videopost.service.VideoSeriesService;
import com.app.features.post.videopost.web.enums.VideoLibraryTab;
import com.app.features.post.videopost.web.view.PublicVideoCardView;
import com.app.features.post.videopost.web.view.PublicVideoDetailPageView;
import com.app.features.post.videopost.web.view.PublicVideoLibraryPageView;
import com.app.features.post.videopost.web.view.PublicVideoPlaylistView;
import com.app.features.post.videopost.web.view.PublicVideoSeriesDetailPageView;
import com.app.features.post.videopost.web.view.PublicVideoSeriesItemView;
import com.app.features.post.videopost.web.view.VideoSeriesCardView;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.view.UiBreadcrumbItemView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.support.SocialShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.videos-path:/videos}")
public class PublicVideoPageController {

    private static final String RESULTS_ID = "video-library-results";

    private static final UiPageDefaults VIDEO_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(12)
                    .sortBy(VideoPostEntity_.POST
                            + "."
                            + PostEntity_.PUBLISHED_AT)
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private static final UiPageDefaults SERIES_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(12)
                    .sortBy(VideoSeriesEntity_.CREATED_AT)
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private static final UiPageDefaults ITEM_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(20)
                    .sortBy(VideoSeriesItemEntity_.POSITION)
                    .sortDirection(Sort.Direction.ASC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final SocialShellFactory socialShellFactory;
    private final VideoPostService videoPostSvc;
    private final VideoSeriesService videoSeriesSvc;
    private final VideoSeriesItemService videoSeriesItemSvc;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @RequestParam(defaultValue = "VIDEOS") VideoLibraryTab tab,
            @RequestParam(required = false) String title,
            @Valid UiPageQuery query,
            Model model) {
        Page<PublicVideoPostResult> videoPage = null;
        Page<VideoSeriesResult> seriesPage = null;
        UiPageDefaults defaults;

        if (tab == VideoLibraryTab.SERIES) {
            VideoSeriesFilterCriteria criteria = new VideoSeriesFilterCriteria();
            criteria.setTitle(title);
            seriesPage = videoSeriesSvc.getPublishedSeries(
                    criteria,
                    query.toPageable(SERIES_PAGE_DEFAULTS));
            defaults = SERIES_PAGE_DEFAULTS;
        } else {
            PublicVideoPostFilterCriteria criteria =
                    new PublicVideoPostFilterCriteria();
            criteria.setTitle(title);
            videoPage = videoPostSvc.getPublishedPosts(
                    criteria,
                    query.toPageable(VIDEO_PAGE_DEFAULTS));
            defaults = VIDEO_PAGE_DEFAULTS;
        }

        Page<?> resultPage = videoPage != null ? videoPage : seriesPage;
        UiPaginationView pagination = uiPaginationFactory.build(
                resultPage,
                uiPaginationPathBuilder.build(
                        getVideosPath(),
                        request,
                        query,
                        defaults),
                UiHtmxNavigationView.forComponent(RESULTS_ID));
        PublicVideoLibraryPageView page =
                PublicVideoLibraryPageView.builder()
                        .title(messageResolver.get("video.public.title"))
                        .shell(socialShellFactory.build(
                                currentUser,
                                request.getRequestURI()))
                        .activeTab(tab)
                        .videosPath(buildTabPath(VideoLibraryTab.VIDEOS))
                        .seriesPath(buildTabPath(VideoLibraryTab.SERIES))
                        .searchPath(buildTabPath(tab))
                        .titleQuery(title)
                        .videos(videoPage == null
                                ? List.of()
                                : videoPage.getContent().stream()
                                        .map(video -> PublicVideoCardView.builder()
                                                .video(video)
                                                .detailPath(buildVideoPath(
                                                        video.getPost().getId()))
                                                .build())
                                        .toList())
                        .series(seriesPage == null
                                ? List.of()
                                : seriesPage.getContent().stream()
                                        .map(series -> VideoSeriesCardView.builder()
                                                .series(series)
                                                .detailPath(buildSeriesPath(
                                                        series.getId()))
                                                .build())
                                        .toList())
                        .pagination(pagination)
                        .build();

        model.addAttribute(PublicVideoLibraryPageView.ATTRIBUTE, page);
        return "post/video/public/index";
    }

    @GetMapping("/{postId}")
    public String videoDetail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @RequestParam(required = false) UUID seriesId,
            HttpServletRequest request,
            @Valid UiPageQuery query,
            Model model) {
        PublicVideoPostResult video = videoPostSvc.getPublishedPost(postId);
        PublicVideoDetailPageView page = PublicVideoDetailPageView.builder()
                .title(messageResolver.get("video.public.detail.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildBreadcrumb(
                        messageResolver.get("video.public.detail.title")))
                .video(video)
                .playlist(seriesId == null
                        ? null
                        : buildPlaylist(
                                seriesId,
                                postId,
                                request,
                                query))
                .build();
        model.addAttribute(PublicVideoDetailPageView.ATTRIBUTE, page);
        return "post/video/public/detail";
    }

    @GetMapping("/series/{seriesId}")
    public String seriesDetail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            HttpServletRequest request,
            @Valid UiPageQuery query,
            Model model) {
        VideoSeriesResult series = videoSeriesSvc.getPublishedSeries(seriesId);
        Page<VideoSeriesItemResult> itemPage =
                videoSeriesItemSvc.getPublishedItems(
                        seriesId,
                        query.toPageable(ITEM_PAGE_DEFAULTS));
        UiPaginationView pagination = uiPaginationFactory.build(
                itemPage,
                uiPaginationPathBuilder.build(
                        buildSeriesPath(seriesId),
                        request,
                        query,
                        ITEM_PAGE_DEFAULTS));
        PublicVideoSeriesDetailPageView page =
                PublicVideoSeriesDetailPageView.builder()
                        .title(series.getTitle())
                        .shell(socialShellFactory.build(
                                currentUser,
                                request.getRequestURI()))
                        .breadcrumb(buildBreadcrumb(series.getTitle()))
                        .series(series)
                        .items(buildSeriesItemViews(
                                itemPage,
                                seriesId,
                                null))
                        .pagination(pagination)
                        .build();
        model.addAttribute(PublicVideoSeriesDetailPageView.ATTRIBUTE, page);
        return "post/video/public/series-detail";
    }

    private PublicVideoPlaylistView buildPlaylist(
            UUID seriesId,
            UUID activePostId,
            HttpServletRequest request,
            UiPageQuery query) {
        VideoSeriesResult series = videoSeriesSvc.getPublishedSeries(seriesId);
        UiPageQuery playlistQuery = query.copy();
        Page<VideoSeriesItemResult> itemPage = videoSeriesItemSvc
                .getPublishedItems(
                        seriesId,
                        playlistQuery.toPageable(ITEM_PAGE_DEFAULTS));
        UiPaginationView pagination = uiPaginationFactory.build(
                itemPage,
                uiPaginationPathBuilder.build(
                        buildVideoPath(activePostId),
                        request,
                        playlistQuery,
                        ITEM_PAGE_DEFAULTS));

        return PublicVideoPlaylistView.builder()
                .series(series)
                .items(buildSeriesItemViews(
                        itemPage,
                        seriesId,
                        activePostId))
                .pagination(pagination)
                .build();
    }

    private List<PublicVideoSeriesItemView> buildSeriesItemViews(
            Page<VideoSeriesItemResult> itemPage,
            UUID seriesId,
            UUID activePostId) {
        return itemPage.getContent().stream()
                .map(item -> PublicVideoSeriesItemView.builder()
                        .item(item)
                        .detailPath(buildSeriesVideoPath(
                                item.getVideo().getId(),
                                seriesId,
                                itemPage.getNumber(),
                                itemPage.getSize()))
                        .active(item.getVideo().getId().equals(activePostId))
                        .build())
                .toList();
    }

    private UiBreadcrumbView buildBreadcrumb(String activeLabel) {
        return UiBreadcrumbView.builder()
                .items(List.of(
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get("video.public.title"))
                                .path(getVideosPath())
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(activeLabel)
                                .active(true)
                                .build()))
                .build();
    }

    private String buildTabPath(VideoLibraryTab tab) {
        return UriComponentsBuilder.fromPath(getVideosPath())
                .queryParam("tab", tab.name())
                .build()
                .encode()
                .toUriString();
    }

    private String buildVideoPath(UUID postId) {
        return UriComponentsBuilder.fromPath(getVideosPath())
                .pathSegment(postId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String buildSeriesVideoPath(
            UUID postId,
            UUID seriesId,
            int page,
            int size) {
        return UriComponentsBuilder.fromPath(buildVideoPath(postId))
                .queryParam("seriesId", seriesId)
                .queryParam("page", page)
                .queryParam("size", size)
                .build()
                .encode()
                .toUriString();
    }

    private String buildSeriesPath(UUID seriesId) {
        return UriComponentsBuilder.fromPath(getVideosPath())
                .pathSegment("series", seriesId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String getVideosPath() {
        return appProperties.getUi().getVideosPath();
    }
}
