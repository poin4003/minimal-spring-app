package com.app.features.post.standard.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.post.entity.PostEntity_;
import com.app.features.post.standard.entity.StandardPostEntity_;
import com.app.features.post.standard.schema.filter.PublicStandardPostFilterCriteria;
import com.app.features.post.standard.schema.result.PublicStandardPostResult;
import com.app.features.post.standard.service.StandardPostService;
import com.app.features.post.standard.web.view.PublicPostCardView;
import com.app.features.post.standard.web.view.PublicPostDetailPageView;
import com.app.features.post.standard.web.view.PublicPostFeedView;
import com.app.features.post.standard.web.view.PublicPostListPageView;
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
@RequestMapping("${app.ui.feed-path:/posts}")
public class PublicStandardPostPageController {

    private static final String PUBLIC_POST_FEED_ID = "public-post-feed";

    private static final UiPageDefaults POST_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(10)
                    .sortBy(StandardPostEntity_.POST
                            + "."
                            + PostEntity_.PUBLISHED_AT)
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final SocialShellFactory socialShellFactory;
    private final StandardPostService standardPostSvc;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            PublicStandardPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        Page<PublicStandardPostResult> postPage =
                standardPostSvc.getPublishedPosts(
                        filter,
                        query.toPageable(POST_PAGE_DEFAULTS));

        UiPaginationView pagination = uiPaginationFactory.build(
                postPage,
                uiPaginationPathBuilder.build(
                        getFeedPath(),
                        request,
                        query,
                        POST_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(
                        PUBLIC_POST_FEED_ID));
        PublicPostFeedView feed = PublicPostFeedView.builder()
                .id(PUBLIC_POST_FEED_ID)
                .posts(postPage.getContent().stream()
                        .map(post -> PublicPostCardView.builder()
                                .post(post)
                                .detailPath(buildDetailPath(post.getId()))
                                .build())
                        .toList())
                .pagination(pagination)
                .build();
        PublicPostListPageView page = PublicPostListPageView.builder()
                .title(messageResolver.get("post.public.feed.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .feed(feed)
                .build();

        model.addAttribute(PublicPostListPageView.ATTRIBUTE, page);
        return "post/standard/public/index";
    }

    @GetMapping("/{postId}")
    public String detail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            Model model) {
        PublicStandardPostResult post =
                standardPostSvc.getPublishedPost(postId);
        PublicPostDetailPageView page =
                PublicPostDetailPageView.builder()
                        .title(messageResolver.get(
                                "post.public.detail.title"))
                        .backPath(getFeedPath())
                        .shell(socialShellFactory.build(
                                currentUser,
                                request.getRequestURI()))
                        .breadcrumb(buildDetailBreadcrumb())
                        .card(PublicPostCardView.builder()
                                .post(post)
                                .detailPath(buildDetailPath(post.getId()))
                                .build())
                        .build();

        model.addAttribute(PublicPostDetailPageView.ATTRIBUTE, page);
        return "post/standard/public/detail";
    }

    private UiBreadcrumbView buildDetailBreadcrumb() {
        return UiBreadcrumbView.builder()
                .items(List.of(
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "post.public.feed.title"))
                                .path(getFeedPath())
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "post.public.detail.title"))
                                .active(true)
                                .build()))
                .build();
    }

    private String buildDetailPath(UUID postId) {
        return UriComponentsBuilder.fromPath(getFeedPath())
                .pathSegment(postId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String getFeedPath() {
        return appProperties.getUi().getFeedPath();
    }
}
