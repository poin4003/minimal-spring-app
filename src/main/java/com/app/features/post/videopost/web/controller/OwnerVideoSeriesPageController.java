package com.app.features.post.videopost.web.controller;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
import com.app.features.post.videopost.entity.VideoPostEntity_;
import com.app.features.post.videopost.enums.VideoSeriesCascadeMode;
import com.app.features.post.videopost.schema.filter.OwnerVideoPostFilterCriteria;
import com.app.features.post.videopost.schema.payload.AddVideoSeriesItemsPayload;
import com.app.features.post.videopost.schema.payload.MoveVideoSeriesItemPayload;
import com.app.features.post.videopost.schema.result.OwnerVideoPostResult;
import com.app.features.post.videopost.schema.result.VideoSeriesItemResult;
import com.app.features.post.videopost.schema.result.VideoSeriesResult;
import com.app.features.post.videopost.service.VideoPostService;
import com.app.features.post.videopost.service.VideoSeriesItemService;
import com.app.features.post.videopost.service.VideoSeriesService;
import com.app.features.post.videopost.web.enums.VideoSeriesActionType;
import com.app.features.post.videopost.web.support.VideoSeriesItemPageSupport;
import com.app.features.post.videopost.web.view.OwnerVideoSeriesCardView;
import com.app.features.post.videopost.web.view.OwnerVideoSeriesDetailPageView;
import com.app.features.post.videopost.web.view.OwnerVideoSeriesItemsPageView;
import com.app.features.post.videopost.web.view.VideoSeriesActionModalView;
import com.app.features.post.videopost.web.view.VideoSeriesActionView;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.view.UiBreadcrumbItemView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.support.SocialShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.my-videos-path:/my/videos}/series")
@Secured(PermissionConstants.POST_VIEW_OWN)
public class OwnerVideoSeriesPageController {

    private static final String ITEMS_ID = "owner-video-series-items";
    private static final String AVAILABLE_ID = "available-series-videos";

    private static final UiPageDefaults VIDEO_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(12)
                    .sortBy(VideoPostEntity_.POST
                            + "."
                            + PostEntity_.CREATED_AT)
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final SocialShellFactory socialShellFactory;
    private final VideoSeriesService videoSeriesSvc;
    private final VideoSeriesItemService videoSeriesItemSvc;
    private final VideoSeriesItemPageSupport videoSeriesItemPageSupport;
    private final VideoPostService videoPostSvc;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder paginationPathBuilder;

    @GetMapping("/{seriesId}")
    public String detail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            HttpServletRequest request,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                OwnerVideoSeriesDetailPageView.ATTRIBUTE,
                buildDetail(
                        currentUser,
                        request,
                        seriesId,
                        query,
                        null,
                        null));
        return "post/video/owner/series-detail";
    }

    @GetMapping("/{seriesId}/items-fragment")
    public String itemsFragment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            HttpServletRequest request,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                OwnerVideoSeriesDetailPageView.ATTRIBUTE,
                buildDetail(
                        currentUser,
                        request,
                        seriesId,
                        query,
                        null,
                        null));
        return "post/video/owner/fragments/series-items"
                + " :: items (page=${page})";
    }

    @GetMapping("/{seriesId}/add-items")
    public String addItems(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            OwnerVideoPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute("form", new AddVideoSeriesItemsPayload());
        model.addAttribute(
                OwnerVideoSeriesItemsPageView.ATTRIBUTE,
                buildAddItemsPage(
                        currentUser,
                        request,
                        seriesId,
                        filter,
                        query));
        return "post/video/owner/add-series-items";
    }

    @GetMapping("/{seriesId}/available-items")
    public String availableItems(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            OwnerVideoPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute("form", new AddVideoSeriesItemsPayload());
        model.addAttribute(
                OwnerVideoSeriesItemsPageView.ATTRIBUTE,
                buildAddItemsPage(
                        currentUser,
                        request,
                        seriesId,
                        filter,
                        query));
        return "post/video/owner/fragments/available-videos"
                + " :: available (page=${page})";
    }

    @PostMapping("/{seriesId}/add-items")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String submitAddItems(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute("form") AddVideoSeriesItemsPayload form,
            BindingResult bindingResult,
            @Valid @ModelAttribute("filter")
            OwnerVideoPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    OwnerVideoSeriesItemsPageView.ATTRIBUTE,
                    buildAddItemsPage(
                            currentUser,
                            request,
                            seriesId,
                            filter,
                            query));
            return "post/video/owner/add-series-items";
        }
        videoSeriesItemSvc.addItems(
                seriesId,
                currentUser.getUserId(),
                form);
        return HtmxRequestSupport.redirectView(
                request,
                response,
                buildDetailPath(seriesId));
    }

    @PostMapping("/{seriesId}/items/{itemId}/remove")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String removeItem(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            @PathVariable UUID itemId,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        videoSeriesItemSvc.removeItem(
                seriesId,
                itemId,
                currentUser.getUserId());
        return itemMutationResponse(
                currentUser,
                request,
                response,
                seriesId,
                query,
                model);
    }

    @PostMapping("/{seriesId}/items/{itemId}/move")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String moveItem(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            @PathVariable UUID itemId,
            @Valid @ModelAttribute("form")
            MoveVideoSeriesItemPayload form,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        videoSeriesItemSvc.moveItem(
                seriesId,
                itemId,
                currentUser.getUserId(),
                form);
        return itemMutationResponse(
                currentUser,
                request,
                response,
                seriesId,
                query,
                model);
    }

    @GetMapping("/{seriesId}/{action}/confirm")
    public String actionConfirm(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            @PathVariable String action,
            HttpServletRequest request,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        VideoSeriesResult series = videoSeriesSvc.getOwnedSeries(
                seriesId,
                currentUser.getUserId());
        VideoSeriesActionType actionType = resolveAction(action);
        if (!supportsAction(series, actionType)) {
            throw ExceptionFactory.invalidParam(
                    "error.videoSeries.actionNotAllowed",
                    action);
        }
        VideoSeriesActionModalView modal = buildActionModal(
                seriesId,
                actionType);
        if (HtmxRequestSupport.isHtmxRequest(request)) {
            model.addAttribute(VideoSeriesActionModalView.ATTRIBUTE, modal);
            return "post/video/owner/fragments/series-action-modal"
                    + " :: modal (modal=${modal})";
        }
        model.addAttribute(
                OwnerVideoSeriesDetailPageView.ATTRIBUTE,
                buildDetail(
                        currentUser,
                        request,
                        seriesId,
                        query,
                        modal,
                        modal.getId()));
        return "post/video/owner/series-detail";
    }

    @PostMapping("/{seriesId}/{action}")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String executeAction(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            @PathVariable String action,
            @RequestParam(defaultValue = "SERIES_ONLY")
            VideoSeriesCascadeMode cascadeMode,
            HttpServletRequest request,
            HttpServletResponse response) {
        UUID ownerId = currentUser.getUserId();
        switch (resolveAction(action)) {
            case ARCHIVE -> videoSeriesSvc.archiveOwnedSeries(
                    seriesId,
                    ownerId,
                    cascadeMode);
            case RESTORE_ARCHIVED -> videoSeriesSvc.restoreArchivedOwnedSeries(
                    seriesId,
                    ownerId,
                    cascadeMode);
            case DELETE -> videoSeriesSvc.deleteOwnedSeries(
                    seriesId,
                    ownerId,
                    cascadeMode);
            case RESTORE_DELETED -> videoSeriesSvc.restoreDeletedOwnedSeries(
                    seriesId,
                    ownerId,
                    cascadeMode);
        }
        return HtmxRequestSupport.redirectView(
                request,
                response,
                getOwnerPath());
    }

    private OwnerVideoSeriesDetailPageView buildDetail(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID seriesId,
            UiPageQuery query,
            VideoSeriesActionModalView modal,
            String openModalId) {
        return buildDetail(
                currentUser,
                request,
                seriesId,
                query,
                modal,
                openModalId,
                true);
    }

    private OwnerVideoSeriesDetailPageView buildDetail(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID seriesId,
            UiPageQuery query,
            VideoSeriesActionModalView modal,
            String openModalId,
            boolean preserveRequestParameters) {
        VideoSeriesResult series = videoSeriesSvc.getOwnedSeries(
                seriesId,
                currentUser.getUserId());
        UiPageQuery itemQuery = videoSeriesItemPageSupport.normalize(query);
        Page<VideoSeriesItemResult> itemPage =
                videoSeriesItemSvc.getOwnedItems(
                        seriesId,
                        currentUser.getUserId(),
                        itemQuery.toPageable(
                                videoSeriesItemPageSupport.getDefaults()));
        String detailPath = buildDetailPath(seriesId);
        IntFunction<String> pagePathBuilder = preserveRequestParameters
                ? paginationPathBuilder.build(
                        detailPath,
                        request,
                        itemQuery,
                        videoSeriesItemPageSupport.getDefaults())
                : buildCleanItemPagePath(detailPath, itemQuery);
        UiPaginationView pagination = uiPaginationFactory.build(
                itemPage,
                pagePathBuilder,
                UiHtmxNavigationView.forComponent(ITEMS_ID));
        return OwnerVideoSeriesDetailPageView.builder()
                .title(series.getTitle())
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildBreadcrumb(series.getTitle()))
                .card(toSeriesCard(series))
                .items(itemPage.getContent())
                .pagination(pagination)
                .addItemsPath(detailPath + "/add-items")
                .createVideoPath(UriComponentsBuilder
                        .fromPath(getOwnerPath() + "/create")
                        .queryParam("seriesId", seriesId)
                        .build()
                        .encode()
                        .toUriString())
                .itemActionPathPrefix(detailPath + "/items/")
                .itemSort(videoSeriesItemPageSupport.buildSort(
                        itemQuery,
                        sortQuery -> sortQuery.toUri(
                                detailPath,
                                videoSeriesItemPageSupport.getDefaults())))
                .actions(buildActions(series))
                .actionModal(modal)
                .openModalId(openModalId)
                .build();
    }

    private String itemMutationResponse(
            UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            UUID seriesId,
            UiPageQuery query,
            Model model) {
        UiPageQuery itemQuery = videoSeriesItemPageSupport.normalize(query);
        OwnerVideoSeriesDetailPageView page = buildDetail(
                currentUser,
                request,
                seriesId,
                itemQuery,
                null,
                null,
                false);
        if (page.getItems().isEmpty()
                && page.getPagination().getCurrentPage() > 0) {
            UiPageQuery previousPageQuery = itemQuery.copy();
            previousPageQuery.setPage(
                    previousPageQuery.getPage() - 1);
            page = buildDetail(
                    currentUser,
                    request,
                    seriesId,
                    previousPageQuery,
                    null,
                    null,
                    false);
            itemQuery = previousPageQuery;
        }

        String currentPath = itemQuery.toUri(
                buildDetailPath(seriesId),
                videoSeriesItemPageSupport.getDefaults());

        if (HtmxRequestSupport.isHtmxRequest(request)) {
            HtmxRequestSupport.replaceUrl(response, currentPath);
            model.addAttribute(
                    OwnerVideoSeriesDetailPageView.ATTRIBUTE,
                    page);
            return "post/video/owner/fragments/series-items"
                    + " :: items (page=${page})";
        }

        return "redirect:" + currentPath;
    }

    private IntFunction<String> buildCleanItemPagePath(
            String detailPath,
            UiPageQuery query) {
        UiPageQuery baseQuery = videoSeriesItemPageSupport.normalize(query);
        return pageNumber -> {
            UiPageQuery pageQuery = baseQuery.copy();
            pageQuery.setPage(pageNumber);
            return pageQuery.toUri(
                    detailPath,
                    videoSeriesItemPageSupport.getDefaults());
        };
    }

    private OwnerVideoSeriesItemsPageView buildAddItemsPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID seriesId,
            OwnerVideoPostFilterCriteria filter,
            UiPageQuery query) {
        VideoSeriesResult series = videoSeriesSvc.getOwnedSeries(
                seriesId,
                currentUser.getUserId());
        filter.setSeriesId(null);
        Page<OwnerVideoPostResult> videoPage = videoPostSvc.getOwnedPosts(
                currentUser.getUserId(),
                filter,
                query.toPageable(VIDEO_PAGE_DEFAULTS));
        String availablePath = buildDetailPath(seriesId)
                + "/add-items";
        UiPaginationView pagination = uiPaginationFactory.build(
                videoPage,
                paginationPathBuilder.build(
                        availablePath,
                        request,
                        query,
                        VIDEO_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(AVAILABLE_ID));
        return OwnerVideoSeriesItemsPageView.builder()
                .title(messageResolver.get("video.series.items.add.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildBreadcrumb(
                        messageResolver.get("video.series.items.add.title")))
                .series(series)
                .videos(videoPage.getContent())
                .actionPath(buildDetailPath(seriesId) + "/add-items")
                .backPath(buildDetailPath(seriesId))
                .searchPath(availablePath)
                .titleQuery(filter.getTitle())
                .pagination(pagination)
                .build();
    }

    private List<VideoSeriesActionView> buildActions(
            VideoSeriesResult series) {
        return Arrays.stream(VideoSeriesActionType.values())
                .filter(action -> supportsAction(series, action))
                .map(action -> VideoSeriesActionView.builder()
                        .label(messageResolver.get(actionLabelKey(action)))
                        .iconClass(actionIcon(action))
                        .buttonClass(actionButtonClass(action))
                        .modalPath(buildDetailPath(series.getId())
                                + "/"
                                + action.getPath()
                                + "/confirm")
                        .build())
                .toList();
    }

    private VideoSeriesActionModalView buildActionModal(
            UUID seriesId,
            VideoSeriesActionType action) {
        return VideoSeriesActionModalView.builder()
                .id("video-series-" + action.getPath() + "-modal")
                .title(messageResolver.get(actionTitleKey(action)))
                .description(messageResolver.get(actionDescriptionKey(action)))
                .actionPath(buildDetailPath(seriesId)
                        + "/"
                        + action.getPath())
                .submitLabel(messageResolver.get(actionLabelKey(action)))
                .submitButtonClass(actionButtonClass(action))
                .build();
    }

    private boolean supportsAction(
            VideoSeriesResult series,
            VideoSeriesActionType action) {
        return switch (series.getLifecycleStatus()) {
            case ACTIVE -> action == VideoSeriesActionType.ARCHIVE
                    || action == VideoSeriesActionType.DELETE;
            case ARCHIVED -> action == VideoSeriesActionType.RESTORE_ARCHIVED
                    || action == VideoSeriesActionType.DELETE;
            case DELETED -> action == VideoSeriesActionType.RESTORE_DELETED;
        };
    }

    private OwnerVideoSeriesCardView toSeriesCard(VideoSeriesResult series) {
        return OwnerVideoSeriesCardView.builder()
                .series(series)
                .detailPath(buildDetailPath(series.getId()))
                .editPath(buildDetailPath(series.getId()) + "/edit")
                .statusLabel(messageResolver.get(
                        "video.series.lifecycleStatus."
                                + series.getLifecycleStatus()
                                        .name()
                                        .toLowerCase()))
                .statusBadgeClass(switch (series.getLifecycleStatus()) {
                    case ACTIVE -> "text-bg-success";
                    case ARCHIVED -> "text-bg-secondary";
                    case DELETED -> "text-bg-danger";
                })
                .build();
    }

    private VideoSeriesActionType resolveAction(String action) {
        return Arrays.stream(VideoSeriesActionType.values())
                .filter(type -> type.getPath().equals(action))
                .findFirst()
                .orElseThrow(() -> ExceptionFactory.invalidParam(
                        "error.videoSeries.actionNotAllowed",
                        action));
    }

    private String actionLabelKey(VideoSeriesActionType action) {
        return switch (action) {
            case ARCHIVE -> "video.series.action.archive";
            case RESTORE_ARCHIVED, RESTORE_DELETED ->
                "video.series.action.restore";
            case DELETE -> "video.series.action.delete";
        };
    }

    private String actionTitleKey(VideoSeriesActionType action) {
        return actionLabelKey(action) + ".title";
    }

    private String actionDescriptionKey(VideoSeriesActionType action) {
        return actionLabelKey(action) + ".description";
    }

    private String actionIcon(VideoSeriesActionType action) {
        return switch (action) {
            case ARCHIVE -> "bi bi-archive";
            case RESTORE_ARCHIVED, RESTORE_DELETED ->
                "bi bi-arrow-counterclockwise";
            case DELETE -> "bi bi-trash";
        };
    }

    private String actionButtonClass(VideoSeriesActionType action) {
        return switch (action) {
            case ARCHIVE -> "btn-outline-secondary";
            case RESTORE_ARCHIVED, RESTORE_DELETED ->
                "btn-outline-success";
            case DELETE -> "btn-outline-danger";
        };
    }

    private UiBreadcrumbView buildBreadcrumb(String label) {
        return UiBreadcrumbView.builder()
                .items(List.of(
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get("video.owner.title"))
                                .path(getOwnerPath())
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(label)
                                .active(true)
                                .build()))
                .build();
    }

    private String buildDetailPath(UUID seriesId) {
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
