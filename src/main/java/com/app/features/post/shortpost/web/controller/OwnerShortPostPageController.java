package com.app.features.post.shortpost.web.controller;

import java.util.List;
import java.util.UUID;

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

import com.app.config.security.web.HtmxRequestSupport;
import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.exception.ExceptionFactory;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.post.entity.PostEntity_;
import com.app.features.post.shortpost.entity.ShortPostEntity_;
import com.app.features.post.shortpost.schema.filter.OwnerShortPostFilterCriteria;
import com.app.features.post.shortpost.schema.result.OwnerShortPostResult;
import com.app.features.post.shortpost.service.ShortPostService;
import com.app.features.post.shortpost.web.support.OwnerShortPostViewFactory;
import com.app.features.post.shortpost.web.view.OwnerShortDetailPageView;
import com.app.features.post.shortpost.web.view.OwnerShortListPageView;
import com.app.features.post.shortpost.web.view.OwnerShortWorkspaceView;
import com.app.features.post.web.enums.OwnerPostActionType;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.view.UiBreadcrumbItemView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiConfirmModalView;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.support.SocialShellFactory;
import com.app.features.user.service.ProfileService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.my-shorts-path:/my/shorts}")
@Secured(PermissionConstants.POST_VIEW_OWN)
public class OwnerShortPostPageController {

    private static final String OWNER_SHORT_WORKSPACE_ID =
            "owner-short-workspace";
    private static final String OWNER_SHORTS_CHANGED_EVENT =
            "ownerShortsChanged";
    private static final String EMPTY_RESPONSE_VIEW =
            "fragments/components/htmx-response :: empty";
    private static final String WORKSPACE_FRAGMENT =
            "post/short/owner/fragments/workspace"
                    + " :: workspace (workspace=${workspace})";

    private static final UiPageDefaults SHORT_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(8)
                    .sortBy(ShortPostEntity_.POST
                            + "."
                            + PostEntity_.CREATED_AT)
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final SocialShellFactory socialShellFactory;
    private final ShortPostService shortPostSvc;
    private final ProfileService profileSvc;
    private final OwnerShortPostViewFactory ownerShortViewFactory;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            OwnerShortPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        OwnerShortListPageView page = OwnerShortListPageView.builder()
                .title(messageResolver.get("short.owner.list.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .profile(profileSvc.getProfile(currentUser.getUserId()))
                .editProfilePath(appProperties.getUi().getProfilePath())
                .standardPostsPath(appProperties.getUi().getMyPostsPath())
                .shortsPath(getMyShortsPath())
                .videosPath(appProperties.getUi().getMyVideosPath())
                .createPath(getMyShortsPath() + "/create")
                .workspace(buildWorkspace(
                        currentUser.getUserId(),
                        request,
                        filter,
                        query))
                .build();

        model.addAttribute(OwnerShortListPageView.ATTRIBUTE, page);
        return "post/short/owner/index";
    }

    @GetMapping("/workspace")
    public String workspace(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            OwnerShortPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                "workspace",
                buildWorkspace(
                        currentUser.getUserId(),
                        request,
                        filter,
                        query));
        return WORKSPACE_FRAGMENT;
    }

    @GetMapping("/{postId}")
    public String detail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            Model model) {
        OwnerShortPostResult post = shortPostSvc.getOwnerPost(
                postId,
                currentUser.getUserId());
        model.addAttribute(
                OwnerShortDetailPageView.ATTRIBUTE,
                buildDetailPage(currentUser, request, post, null, null));
        return "post/short/owner/detail";
    }

    @GetMapping("/{postId}/submit/confirm")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String submitConfirm(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            Model model) {
        return showActionConfirm(currentUser, postId,
                OwnerPostActionType.SUBMIT, detail, request, model);
    }

    @GetMapping("/{postId}/archive/confirm")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String archiveConfirm(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            Model model) {
        return showActionConfirm(currentUser, postId,
                OwnerPostActionType.ARCHIVE, detail, request, model);
    }

    @GetMapping("/{postId}/restore-archived/confirm")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String restoreArchivedConfirm(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            Model model) {
        return showActionConfirm(currentUser, postId,
                OwnerPostActionType.RESTORE_ARCHIVED,
                detail, request, model);
    }

    @GetMapping("/{postId}/delete/confirm")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String deleteConfirm(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            Model model) {
        return showActionConfirm(currentUser, postId,
                OwnerPostActionType.DELETE, detail, request, model);
    }

    @GetMapping("/{postId}/restore-deleted/confirm")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String restoreDeletedConfirm(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            Model model) {
        return showActionConfirm(currentUser, postId,
                OwnerPostActionType.RESTORE_DELETED,
                detail, request, model);
    }

    private String showActionConfirm(
            UserPrincipal currentUser,
            UUID postId,
            OwnerPostActionType action,
            boolean detail,
            HttpServletRequest request,
            Model model) {
        OwnerShortPostResult post = shortPostSvc.getOwnerPost(
                postId,
                currentUser.getUserId());
        if (!ownerShortViewFactory.supportsAction(post, action)) {
            throw ExceptionFactory.invalidParam(
                    "error.post.lifecycleInvalid",
                    postId);
        }

        UiConfirmModalView modal = ownerShortViewFactory.buildActionModal(
                post,
                action,
                detail);
        if (HtmxRequestSupport.isHtmxRequest(request)) {
            model.addAttribute(UiConfirmModalView.ATTRIBUTE, modal);
            return "fragments/components/confirm-modal"
                    + " :: modal (modal=${modal})";
        }

        model.addAttribute(
                OwnerShortDetailPageView.ATTRIBUTE,
                buildDetailPage(
                        currentUser,
                        request,
                        post,
                        modal,
                        modal.getId()));
        return "post/short/owner/detail";
    }

    @PostMapping("/{postId}/submit")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String submitForReview(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            HttpServletResponse response) {
        shortPostSvc.submitOwnedPostForReview(
                postId,
                currentUser.getUserId());
        return completeAction(request, response, detail);
    }

    @PostMapping("/{postId}/archive")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String archivePost(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            HttpServletResponse response) {
        shortPostSvc.archiveOwnedPost(
                postId,
                currentUser.getUserId());
        return completeAction(request, response, detail);
    }

    @PostMapping("/{postId}/restore-archived")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String restoreArchived(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            HttpServletResponse response) {
        shortPostSvc.restoreArchivedOwnedPost(
                postId,
                currentUser.getUserId());
        return completeAction(request, response, detail);
    }

    @PostMapping("/{postId}/delete")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String deletePost(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            HttpServletResponse response) {
        shortPostSvc.deleteOwnedPost(
                postId,
                currentUser.getUserId());
        return completeAction(request, response, detail);
    }

    @PostMapping("/{postId}/restore-deleted")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String restoreDeleted(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            HttpServletResponse response) {
        shortPostSvc.restoreDeletedOwnedPost(
                postId,
                currentUser.getUserId());
        return completeAction(request, response, detail);
    }

    private String completeAction(
            HttpServletRequest request,
            HttpServletResponse response,
            boolean detail) {
        if (HtmxRequestSupport.isHtmxRequest(request) && !detail) {
            HtmxRequestSupport.trigger(
                    response,
                    OWNER_SHORTS_CHANGED_EVENT);
            return EMPTY_RESPONSE_VIEW;
        }
        return HtmxRequestSupport.redirectView(
                request,
                response,
                getMyShortsPath());
    }

    private OwnerShortWorkspaceView buildWorkspace(
            UUID ownerId,
            HttpServletRequest request,
            OwnerShortPostFilterCriteria filter,
            UiPageQuery query) {
        Page<OwnerShortPostResult> shortPage = shortPostSvc.getOwnedPosts(
                ownerId,
                filter,
                query.toPageable(SHORT_PAGE_DEFAULTS));
        UiPageQuery resolvedQuery = query.applyDefaults(
                SHORT_PAGE_DEFAULTS);
        UiPaginationView pagination = uiPaginationFactory.build(
                shortPage,
                uiPaginationPathBuilder.build(
                        getMyShortsPath(),
                        request,
                        query,
                        SHORT_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(
                        OWNER_SHORT_WORKSPACE_ID));
        String refreshPath = uiPaginationPathBuilder.build(
                        getMyShortsPath() + "/workspace",
                        request,
                        resolvedQuery,
                        SHORT_PAGE_DEFAULTS)
                .apply(resolvedQuery.getPage());

        return OwnerShortWorkspaceView.builder()
                .id(OWNER_SHORT_WORKSPACE_ID)
                .refreshPath(refreshPath)
                .refreshEvent(OWNER_SHORTS_CHANGED_EVENT)
                .statusFilters(ownerShortViewFactory.buildStatusFilters(
                        filter.getLifecycleStatus(),
                        filter.getModerationStatus()))
                .shorts(shortPage.getContent().stream()
                        .map(post -> ownerShortViewFactory.toCard(post, false))
                        .toList())
                .pagination(pagination)
                .build();
    }

    private OwnerShortDetailPageView buildDetailPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            OwnerShortPostResult post,
            UiConfirmModalView actionModal,
            String openModalId) {
        return OwnerShortDetailPageView.builder()
                .title(messageResolver.get("short.owner.detail.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildDetailBreadcrumb())
                .card(ownerShortViewFactory.toCard(post, true))
                .actionModal(actionModal)
                .openModalId(openModalId)
                .build();
    }

    private UiBreadcrumbView buildDetailBreadcrumb() {
        return UiBreadcrumbView.builder()
                .items(List.of(
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "short.owner.list.title"))
                                .path(getMyShortsPath())
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "short.owner.detail.title"))
                                .active(true)
                                .build()))
                .build();
    }

    private String getMyShortsPath() {
        return appProperties.getUi().getMyShortsPath();
    }
}
