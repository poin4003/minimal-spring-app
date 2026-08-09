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
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.post.entity.PostEntity_;
import com.app.features.post.standard.entity.StandardPostEntity_;
import com.app.features.post.standard.schema.filter.OwnerStandardPostFilterCriteria;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.service.StandardPostService;
import com.app.features.post.standard.web.support.OwnerStandardPostViewFactory;
import com.app.features.post.standard.web.view.OwnerPostDetailPageView;
import com.app.features.post.standard.web.view.OwnerPostListPageView;
import com.app.features.post.standard.web.view.OwnerPostWorkspaceView;
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
@RequestMapping("${app.ui.my-posts-path:/my/posts}")
@Secured(PermissionConstants.POST_VIEW_OWN)
public class OwnerStandardPostPageController {

    private static final String OWNER_POST_WORKSPACE_ID =
            "owner-post-workspace";

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
        Page<OwnerStandardPostResult> postPage =
                standardPostSvc.getOwnedPosts(
                        currentUser.getUserId(),
                        filter,
                        query.toPageable(POST_PAGE_DEFAULTS));

        UiPaginationView pagination = uiPaginationFactory.build(
                postPage,
                uiPaginationPathBuilder.build(
                        getMyPostsPath(),
                        request,
                        query,
                        POST_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(
                        OWNER_POST_WORKSPACE_ID));
        OwnerPostWorkspaceView workspace =
                OwnerPostWorkspaceView.builder()
                        .id(OWNER_POST_WORKSPACE_ID)
                        .statusFilters(ownerPostViewFactory
                                .buildStatusFilters(
                                        filter.getModerationStatus()))
                        .posts(postPage.getContent().stream()
                                .map(post -> ownerPostViewFactory.toCard(post))
                                .toList())
                        .pagination(pagination)
                        .build();
        OwnerPostListPageView page = OwnerPostListPageView.builder()
                .title(messageResolver.get("post.owner.list.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .createPath(getMyPostsPath() + "/create")
                .workspace(workspace)
                .build();

        model.addAttribute(OwnerPostListPageView.ATTRIBUTE, page);
        return "post/standard/owner/index";
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
        OwnerPostDetailPageView page =
                OwnerPostDetailPageView.builder()
                        .title(messageResolver.get(
                                "post.owner.detail.title"))
                        .shell(socialShellFactory.build(
                                currentUser,
                                request.getRequestURI()))
                        .breadcrumb(buildDetailBreadcrumb())
                        .card(ownerPostViewFactory.toCard(post))
                        .build();

        model.addAttribute(OwnerPostDetailPageView.ATTRIBUTE, page);
        return "post/standard/owner/detail";
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
