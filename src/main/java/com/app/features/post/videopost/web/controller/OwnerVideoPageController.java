package com.app.features.post.videopost.web.controller;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.security.web.HtmxRequestSupport;
import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.exception.ExceptionFactory;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.post.entity.PostEntity_;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.videopost.entity.VideoPostEntity_;
import com.app.features.post.videopost.entity.VideoSeriesEntity_;
import com.app.features.post.videopost.enums.VideoSeriesLifecycleStatus;
import com.app.features.post.videopost.schema.filter.OwnerVideoPostFilterCriteria;
import com.app.features.post.videopost.schema.filter.VideoSeriesFilterCriteria;
import com.app.features.post.videopost.schema.result.OwnerVideoPostResult;
import com.app.features.post.videopost.schema.result.VideoSeriesResult;
import com.app.features.post.videopost.service.VideoPostService;
import com.app.features.post.videopost.service.VideoSeriesService;
import com.app.features.post.videopost.web.enums.VideoLibraryTab;
import com.app.features.post.videopost.web.support.OwnerVideoPostViewFactory;
import com.app.features.post.videopost.web.view.OwnerVideoDetailPageView;
import com.app.features.post.videopost.web.view.OwnerVideoLibraryPageView;
import com.app.features.post.videopost.web.view.OwnerVideoSeriesCardView;
import com.app.features.post.web.enums.OwnerPostActionType;
import com.app.features.post.web.view.OwnerPostStatusFilterView;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.view.UiBreadcrumbItemView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiConfirmModalView;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.support.SocialShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.my-videos-path:/my/videos}")
@Secured(PermissionConstants.POST_VIEW_OWN)
public class OwnerVideoPageController {

    private static final String RESULTS_ID = "owner-video-results";
    private static final String CHANGED_EVENT = "ownerVideosChanged";
    private static final String EMPTY_RESPONSE_VIEW =
            "fragments/components/htmx-response :: empty";

    private static final UiPageDefaults VIDEO_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(12)
                    .sortBy(VideoPostEntity_.POST
                            + "."
                            + PostEntity_.CREATED_AT)
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private static final UiPageDefaults SERIES_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(12)
                    .sortBy(VideoSeriesEntity_.CREATED_AT)
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final SocialShellFactory socialShellFactory;
    private final VideoPostService videoPostSvc;
    private final VideoSeriesService videoSeriesSvc;
    private final OwnerVideoPostViewFactory videoViewFactory;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder paginationPathBuilder;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @RequestParam(defaultValue = "VIDEOS") VideoLibraryTab tab,
            @RequestParam(required = false) String title,
            @RequestParam(required = false)
            PostLifecycleStatus lifecycleStatus,
            @RequestParam(required = false)
            PostModerationStatus moderationStatus,
            @RequestParam(required = false)
            VideoSeriesLifecycleStatus seriesStatus,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                OwnerVideoLibraryPageView.ATTRIBUTE,
                buildLibrary(
                        currentUser,
                        request,
                        tab,
                        title,
                        lifecycleStatus,
                        moderationStatus,
                        seriesStatus,
                        query));
        return "post/video/owner/index";
    }

    @GetMapping("/results")
    public String results(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @RequestParam(defaultValue = "VIDEOS") VideoLibraryTab tab,
            @RequestParam(required = false) String title,
            @RequestParam(required = false)
            PostLifecycleStatus lifecycleStatus,
            @RequestParam(required = false)
            PostModerationStatus moderationStatus,
            @RequestParam(required = false)
            VideoSeriesLifecycleStatus seriesStatus,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                OwnerVideoLibraryPageView.ATTRIBUTE,
                buildLibrary(
                        currentUser,
                        request,
                        tab,
                        title,
                        lifecycleStatus,
                        moderationStatus,
                        seriesStatus,
                        query));
        return "post/video/owner/fragments/results"
                + " :: results (page=${page})";
    }

    @GetMapping("/{postId}")
    public String detail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            Model model) {
        model.addAttribute(
                OwnerVideoDetailPageView.ATTRIBUTE,
                buildDetail(currentUser, request, postId, null, null));
        return "post/video/owner/detail";
    }

    @GetMapping("/{postId}/{action}/confirm")
    public String actionConfirm(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @PathVariable String action,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            Model model) {
        OwnerPostActionType actionType = resolveAction(action);
        OwnerVideoPostResult video = videoPostSvc.getOwnerPost(
                postId,
                currentUser.getUserId());
        UiConfirmModalView modal = videoViewFactory.buildActionModal(
                video,
                actionType,
                detail);
        if (modal == null) {
            throw ExceptionFactory.invalidParam(
                    "error.post.actionNotAllowed",
                    action);
        }
        if (HtmxRequestSupport.isHtmxRequest(request)) {
            model.addAttribute(UiConfirmModalView.ATTRIBUTE, modal);
            return "fragments/components/confirm-modal"
                    + " :: modal (modal=${modal})";
        }

        model.addAttribute(
                OwnerVideoDetailPageView.ATTRIBUTE,
                buildDetail(
                        currentUser,
                        request,
                        postId,
                        modal,
                        modal.getId()));
        return "post/video/owner/detail";
    }

    @PostMapping("/{postId}/{action}")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String executeAction(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @PathVariable String action,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            HttpServletResponse response) {
        UUID ownerId = currentUser.getUserId();
        switch (resolveAction(action)) {
            case SUBMIT -> videoPostSvc.submitOwnedPostForReview(
                    postId,
                    ownerId);
            case ARCHIVE -> videoPostSvc.archiveOwnedPost(postId, ownerId);
            case RESTORE_ARCHIVED -> videoPostSvc.restoreArchivedOwnedPost(
                    postId,
                    ownerId);
            case DELETE -> videoPostSvc.deleteOwnedPost(postId, ownerId);
            case RESTORE_DELETED -> videoPostSvc.restoreDeletedOwnedPost(
                    postId,
                    ownerId);
        }
        return completeAction(
                request,
                response,
                detail ? buildDetailPath(postId) : getOwnerPath());
    }

    private OwnerVideoLibraryPageView buildLibrary(
            UserPrincipal currentUser,
            HttpServletRequest request,
            VideoLibraryTab tab,
            String title,
            PostLifecycleStatus lifecycleStatus,
            PostModerationStatus moderationStatus,
            VideoSeriesLifecycleStatus seriesStatus,
            UiPageQuery query) {
        Page<OwnerVideoPostResult> videoPage = null;
        Page<VideoSeriesResult> seriesPage = null;
        UiPageDefaults defaults;

        if (tab == VideoLibraryTab.SERIES) {
            VideoSeriesFilterCriteria seriesFilter =
                    new VideoSeriesFilterCriteria();
            seriesFilter.setTitle(title);
            seriesFilter.setLifecycleStatus(seriesStatus);
            seriesPage = videoSeriesSvc.getOwnedSeries(
                    currentUser.getUserId(),
                    seriesFilter,
                    query.toPageable(SERIES_PAGE_DEFAULTS));
            defaults = SERIES_PAGE_DEFAULTS;
        } else {
            OwnerVideoPostFilterCriteria videoFilter =
                    new OwnerVideoPostFilterCriteria();
            videoFilter.setTitle(title);
            videoFilter.setLifecycleStatus(lifecycleStatus);
            videoFilter.setModerationStatus(moderationStatus);
            videoPage = videoPostSvc.getOwnedPosts(
                    currentUser.getUserId(),
                    videoFilter,
                    query.toPageable(VIDEO_PAGE_DEFAULTS));
            defaults = VIDEO_PAGE_DEFAULTS;
        }

        Page<?> resultPage = videoPage != null ? videoPage : seriesPage;
        UiPaginationView pagination = uiPaginationFactory.build(
                resultPage,
                paginationPathBuilder.build(
                        buildTabPath(tab),
                        request,
                        query,
                        defaults),
                UiHtmxNavigationView.forComponent(RESULTS_ID));
        return OwnerVideoLibraryPageView.builder()
                .title(messageResolver.get("video.owner.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .activeTab(tab)
                .videosPath(buildTabPath(VideoLibraryTab.VIDEOS))
                .seriesPath(buildTabPath(VideoLibraryTab.SERIES))
                .searchPath(buildTabPath(tab))
                .createVideoPath(getOwnerPath() + "/create")
                .createSeriesPath(getOwnerPath() + "/series/create")
                .titleQuery(title)
                .videoStatusFilters(videoViewFactory.buildStatusFilters(
                        lifecycleStatus,
                        moderationStatus))
                .seriesStatusFilters(buildSeriesStatusFilters(
                        seriesStatus))
                .videoLifecycleStatus(lifecycleStatus)
                .videoModerationStatus(moderationStatus)
                .seriesLifecycleStatus(seriesStatus)
                .videos(videoPage == null
                        ? List.of()
                        : videoPage.getContent().stream()
                                .map(video -> videoViewFactory.toCard(
                                        video,
                                        false))
                                .toList())
                .series(seriesPage == null
                        ? List.of()
                        : seriesPage.getContent().stream()
                                .map(series -> toSeriesCard(series))
                                .toList())
                .pagination(pagination)
                .build();
    }

    private List<OwnerPostStatusFilterView> buildSeriesStatusFilters(
            VideoSeriesLifecycleStatus currentStatus) {
        return Stream.concat(
                        Stream.of(
                                OwnerPostStatusFilterView.builder()
                                        .label(messageResolver.get(
                                                "post.owner.filter.all"))
                                        .path(buildTabPath(
                                                VideoLibraryTab.SERIES))
                                        .active(currentStatus == null)
                                        .build()),
                        Arrays.stream(
                                VideoSeriesLifecycleStatus.values())
                                .map(status -> OwnerPostStatusFilterView
                                        .builder()
                                        .label(messageResolver.get(
                                                "video.series.lifecycleStatus."
                                                        + status.name()
                                                                .toLowerCase()))
                                        .path(UriComponentsBuilder
                                                .fromPath(getOwnerPath())
                                                .queryParam(
                                                        "tab",
                                                        VideoLibraryTab.SERIES)
                                                .queryParam(
                                                "seriesStatus",
                                                        status)
                                                .build()
                                                .encode()
                                                .toUriString())
                                        .active(status == currentStatus)
                                        .build()))
                .toList();
    }

    private OwnerVideoDetailPageView buildDetail(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID postId,
            UiConfirmModalView modal,
            String openModalId) {
        OwnerVideoPostResult video = videoPostSvc.getOwnerPost(
                postId,
                currentUser.getUserId());
        return OwnerVideoDetailPageView.builder()
                .title(video.getTitle())
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildBreadcrumb(video.getTitle()))
                .card(videoViewFactory.toCard(video, true))
                .actionModal(modal)
                .openModalId(openModalId)
                .build();
    }

    private OwnerVideoSeriesCardView toSeriesCard(VideoSeriesResult series) {
        String statusKey = "video.series.lifecycleStatus."
                + series.getLifecycleStatus().name().toLowerCase();
        String badgeClass = switch (series.getLifecycleStatus()) {
            case ACTIVE -> "text-bg-success";
            case ARCHIVED -> "text-bg-secondary";
            case DELETED -> "text-bg-danger";
        };
        return OwnerVideoSeriesCardView.builder()
                .series(series)
                .detailPath(buildSeriesDetailPath(series.getId()))
                .editPath(buildSeriesDetailPath(series.getId()) + "/edit")
                .statusLabel(messageResolver.get(statusKey))
                .statusBadgeClass(badgeClass)
                .build();
    }

    private String completeAction(
            HttpServletRequest request,
            HttpServletResponse response,
            String fallbackPath) {
        if (HtmxRequestSupport.isHtmxRequest(request)) {
            HtmxRequestSupport.trigger(response, CHANGED_EVENT);
            HtmxRequestSupport.redirect(response, fallbackPath);
            return EMPTY_RESPONSE_VIEW;
        }
        return "redirect:" + fallbackPath;
    }

    private OwnerPostActionType resolveAction(String action) {
        return java.util.Arrays.stream(OwnerPostActionType.values())
                .filter(type -> type.getPath().equals(action))
                .findFirst()
                .orElseThrow(() -> ExceptionFactory.invalidParam(
                        "error.post.actionNotAllowed",
                        action));
    }

    private UiBreadcrumbView buildBreadcrumb(String label) {
        return UiBreadcrumbView.builder()
                .items(List.of(
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "video.owner.title"))
                                .path(getOwnerPath())
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(label)
                                .active(true)
                                .build()))
                .build();
    }

    private String buildTabPath(VideoLibraryTab tab) {
        return UriComponentsBuilder.fromPath(getOwnerPath())
                .queryParam("tab", tab.name())
                .build()
                .encode()
                .toUriString();
    }

    private String buildDetailPath(UUID postId) {
        return UriComponentsBuilder.fromPath(getOwnerPath())
                .pathSegment(postId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String buildSeriesDetailPath(UUID seriesId) {
        return UriComponentsBuilder.fromPath(getOwnerPath())
                .pathSegment("series", seriesId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String getOwnerPath() {
        return appProperties.getUi().getMyVideosPath();
    }
}
