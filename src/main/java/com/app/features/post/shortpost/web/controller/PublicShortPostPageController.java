package com.app.features.post.shortpost.web.controller;

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
import com.app.features.post.shortpost.entity.ShortPostEntity_;
import com.app.features.post.shortpost.schema.filter.PublicShortPostFilterCriteria;
import com.app.features.post.shortpost.schema.result.PublicShortPostResult;
import com.app.features.post.shortpost.service.ShortPostService;
import com.app.features.post.shortpost.web.view.PublicShortCardView;
import com.app.features.post.shortpost.web.view.PublicShortDetailPageView;
import com.app.features.post.shortpost.web.view.PublicShortFeedView;
import com.app.features.post.shortpost.web.view.PublicShortListPageView;
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
@RequestMapping("${app.ui.shorts-path:/shorts}")
public class PublicShortPostPageController {

    private static final String PUBLIC_SHORT_FEED_ID = "public-short-feed";

    private static final UiPageDefaults SHORT_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(8)
                    .sortBy(ShortPostEntity_.POST
                            + "."
                            + PostEntity_.PUBLISHED_AT)
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final SocialShellFactory socialShellFactory;
    private final ShortPostService shortPostSvc;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            PublicShortPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        Page<PublicShortPostResult> shortPage =
                shortPostSvc.getPublishedPosts(
                        filter,
                        query.toPageable(SHORT_PAGE_DEFAULTS));
        UiPaginationView pagination = uiPaginationFactory.build(
                shortPage,
                uiPaginationPathBuilder.build(
                        getShortsPath(),
                        request,
                        query,
                        SHORT_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(
                        PUBLIC_SHORT_FEED_ID));
        PublicShortFeedView feed = PublicShortFeedView.builder()
                .id(PUBLIC_SHORT_FEED_ID)
                .shorts(shortPage.getContent().stream()
                        .map(this::toCard)
                        .toList())
                .pagination(pagination)
                .build();
        PublicShortListPageView page = PublicShortListPageView.builder()
                .title(messageResolver.get("short.public.feed.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .createPath(currentUser == null
                        ? null
                        : getMyShortsPath() + "/create")
                .feed(feed)
                .build();

        model.addAttribute(PublicShortListPageView.ATTRIBUTE, page);
        return "post/short/public/index";
    }

    @GetMapping("/{postId}")
    public String detail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            Model model) {
        PublicShortDetailPageView page = PublicShortDetailPageView.builder()
                .title(messageResolver.get("short.public.detail.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildDetailBreadcrumb())
                .card(toCard(shortPostSvc.getPublishedPost(postId)))
                .build();

        model.addAttribute(PublicShortDetailPageView.ATTRIBUTE, page);
        return "post/short/public/detail";
    }

    private PublicShortCardView toCard(PublicShortPostResult post) {
        return PublicShortCardView.builder()
                .post(post)
                .detailPath(buildDetailPath(post.getPost().getId()))
                .build();
    }

    private UiBreadcrumbView buildDetailBreadcrumb() {
        return UiBreadcrumbView.builder()
                .items(List.of(
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "short.public.feed.title"))
                                .path(getShortsPath())
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "short.public.detail.title"))
                                .active(true)
                                .build()))
                .build();
    }

    private String buildDetailPath(UUID postId) {
        return UriComponentsBuilder.fromPath(getShortsPath())
                .pathSegment(postId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String getShortsPath() {
        return appProperties.getUi().getShortsPath();
    }

    private String getMyShortsPath() {
        return appProperties.getUi().getMyShortsPath();
    }
}
