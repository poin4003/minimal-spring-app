package com.app.features.post.standard.web.controller;

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
import com.app.features.post.standard.entity.StandardPostEntity_;
import com.app.features.post.standard.schema.filter.OwnerStandardPostFilterCriteria;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.service.StandardPostService;
import com.app.features.post.web.enums.OwnerPostActionType;
import com.app.features.post.standard.web.support.OwnerStandardPostViewFactory;
import com.app.features.post.standard.web.view.OwnerPostDetailPageView;
import com.app.features.post.standard.web.view.OwnerPostListPageView;
import com.app.features.post.standard.web.view.OwnerPostWorkspaceView;
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
@RequestMapping("${app.ui.my-posts-path:/my/posts}")
@Secured(PermissionConstants.POST_VIEW_OWN)
public class OwnerStandardPostPageController {

    private static final String OWNER_POST_WORKSPACE_ID =
            "owner-post-workspace";
    private static final String OWNER_POSTS_CHANGED_EVENT =
            "ownerPostsChanged";
    private static final String EMPTY_RESPONSE_VIEW =
            "fragments/components/htmx-response :: empty";
    private static final String WORKSPACE_FRAGMENT =
            "post/standard/owner/fragments/workspace"
                    + " :: workspace (workspace=${workspace})";

    private static final UiPageDefaults POST_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(10)
                    .sortBy(StandardPostEntity_.POST
                            + "."
                            + PostEntity_.CREATED_AT)
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final SocialShellFactory socialShellFactory;
    private final StandardPostService standardPostSvc;
    private final ProfileService profileSvc;
    private final OwnerStandardPostViewFactory ownerPostViewFactory;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            OwnerStandardPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        OwnerPostWorkspaceView workspace = buildWorkspace(
                currentUser.getUserId(),
                request,
                filter,
                query);
        OwnerPostListPageView page = OwnerPostListPageView.builder()
                .title(messageResolver.get("profile.personal.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .profile(profileSvc.getProfile(
                        currentUser.getUserId()))
                .editProfilePath(
                        appProperties.getUi().getProfilePath())
                .shortsPath(appProperties.getUi().getMyShortsPath())
                .createPath(getMyPostsPath() + "/create")
                .workspace(workspace)
                .build();

        model.addAttribute(OwnerPostListPageView.ATTRIBUTE, page);
        return "post/standard/owner/index";
    }

    @GetMapping("/workspace")
    public String workspace(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            OwnerStandardPostFilterCriteria filter,
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
        OwnerStandardPostResult post = standardPostSvc.getOwnerPost(
                postId,
                currentUser.getUserId());
        OwnerPostDetailPageView page = buildDetailPage(
                currentUser,
                request,
                post,
                null,
                null);

        model.addAttribute(OwnerPostDetailPageView.ATTRIBUTE, page);
        return "post/standard/owner/detail";
    }

    @GetMapping("/{postId}/actions/{actionPath}/confirm")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String actionConfirm(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @PathVariable String actionPath,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            Model model) {
        OwnerPostActionType action = resolveAction(actionPath, postId);
        OwnerStandardPostResult post = standardPostSvc.getOwnerPost(
                postId,
                currentUser.getUserId());
        if (!ownerPostViewFactory.supportsAction(post, action)) {
            throw ExceptionFactory.invalidParam(
                    "error.post.lifecycleInvalid",
                    postId);
        }

        UiConfirmModalView modal = ownerPostViewFactory.buildActionModal(
                post,
                action,
                detail);
        if (HtmxRequestSupport.isHtmxRequest(request)) {
            model.addAttribute(UiConfirmModalView.ATTRIBUTE, modal);
            return "fragments/components/confirm-modal"
                    + " :: modal (modal=${modal})";
        }

        model.addAttribute(
                OwnerPostDetailPageView.ATTRIBUTE,
                buildDetailPage(
                        currentUser,
                        request,
                        post,
                        modal,
                        modal.getId()));
        return "post/standard/owner/detail";
    }

    @PostMapping("/{postId}/actions/{actionPath}")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String performAction(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            @PathVariable String actionPath,
            @RequestParam(defaultValue = "false") boolean detail,
            HttpServletRequest request,
            HttpServletResponse response) {
        OwnerPostActionType action = resolveAction(actionPath, postId);
        UUID ownerId = currentUser.getUserId();
        switch (action) {
            case SUBMIT -> standardPostSvc.submitOwnedPostForReview(
                    postId,
                    ownerId);
            case ARCHIVE -> standardPostSvc.archiveOwnedPost(
                    postId,
                    ownerId);
            case RESTORE_ARCHIVED ->
                standardPostSvc.restoreArchivedOwnedPost(
                        postId,
                        ownerId);
            case DELETE -> standardPostSvc.deleteOwnedPost(
                    postId,
                    ownerId);
            case RESTORE_DELETED ->
                standardPostSvc.restoreDeletedOwnedPost(
                        postId,
                        ownerId);
        }

        if (HtmxRequestSupport.isHtmxRequest(request) && !detail) {
            HtmxRequestSupport.trigger(
                    response,
                    OWNER_POSTS_CHANGED_EVENT);
            return EMPTY_RESPONSE_VIEW;
        }
        return HtmxRequestSupport.redirectView(
                request,
                response,
                getMyPostsPath());
    }

    private OwnerPostWorkspaceView buildWorkspace(
            UUID ownerId,
            HttpServletRequest request,
            OwnerStandardPostFilterCriteria filter,
            UiPageQuery query) {
        Page<OwnerStandardPostResult> postPage =
                standardPostSvc.getOwnedPosts(
                        ownerId,
                        filter,
                        query.toPageable(POST_PAGE_DEFAULTS));
        UiPageQuery resolvedQuery = query.applyDefaults(
                POST_PAGE_DEFAULTS);
        UiPaginationView pagination = uiPaginationFactory.build(
                postPage,
                uiPaginationPathBuilder.build(
                        getMyPostsPath(),
                        request,
                        query,
                        POST_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(
                        OWNER_POST_WORKSPACE_ID));
        String refreshPath = uiPaginationPathBuilder.build(
                        getMyPostsPath() + "/workspace",
                        request,
                        resolvedQuery,
                        POST_PAGE_DEFAULTS)
                .apply(resolvedQuery.getPage());

        return OwnerPostWorkspaceView.builder()
                .id(OWNER_POST_WORKSPACE_ID)
                .refreshPath(refreshPath)
                .refreshEvent(OWNER_POSTS_CHANGED_EVENT)
                .statusFilters(ownerPostViewFactory
                        .buildStatusFilters(
                                filter.getLifecycleStatus(),
                                filter.getModerationStatus()))
                .posts(postPage.getContent().stream()
                        .map(post -> ownerPostViewFactory.toCard(
                                post,
                                false))
                        .toList())
                .pagination(pagination)
                .build();
    }

    private OwnerPostDetailPageView buildDetailPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            OwnerStandardPostResult post,
            UiConfirmModalView actionModal,
            String openModalId) {
        return OwnerPostDetailPageView.builder()
                .title(messageResolver.get(
                        "post.owner.detail.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildDetailBreadcrumb())
                .card(ownerPostViewFactory.toCard(post, true))
                .actionModal(actionModal)
                .openModalId(openModalId)
                .build();
    }

    private OwnerPostActionType resolveAction(
            String actionPath,
            UUID postId) {
        OwnerPostActionType action = OwnerPostActionType.fromPath(
                actionPath);
        if (action == null) {
            throw ExceptionFactory.invalidParam(
                    "error.post.lifecycleInvalid",
                    postId);
        }
        return action;
    }

    private UiBreadcrumbView buildDetailBreadcrumb() {
        return UiBreadcrumbView.builder()
                .items(List.of(
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "post.owner.list.title"))
                                .path(getMyPostsPath())
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "post.owner.detail.title"))
                                .active(true)
                                .build()))
                .build();
    }

    private String getMyPostsPath() {
        return appProperties.getUi().getMyPostsPath();
    }
}
