package com.app.features.post.shortpost.web.controller;

import java.util.ArrayList;
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
import com.app.features.post.shortpost.web.view.PublicShortDetailFeedView;
import com.app.features.post.shortpost.web.view.PublicShortDetailPageView;
import com.app.features.post.shortpost.web.view.PublicShortGalleryView;
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

    private static final String PUBLIC_SHORT_GALLERY_ID =
            "public-short-gallery";
    private static final String PUBLIC_SHORT_DETAIL_FEED_ID =
            "public-short-detail-feed";

    private static final UiPageDefaults SHORT_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(10)
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
        query.setSize(SHORT_PAGE_DEFAULTS.getSize());
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
                        PUBLIC_SHORT_GALLERY_ID));
        PublicShortGalleryView gallery =
                PublicShortGalleryView.builder()
                        .id(PUBLIC_SHORT_GALLERY_ID)
                        .shorts(shortPage.getContent().stream()
                                .map(post -> toCard(
                                        post,
                                        shortPage.getNumber()))
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
                .gallery(gallery)
                .build();

        model.addAttribute(PublicShortListPageView.ATTRIBUTE, page);
        return "post/short/public/index";
    }

    @GetMapping("/{postId}")
    public String detail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            PublicShortPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        query.setSize(SHORT_PAGE_DEFAULTS.getSize());
        Page<PublicShortPostResult> shortPage =
                shortPostSvc.getPublishedPosts(
                        filter,
                        query.toPageable(SHORT_PAGE_DEFAULTS));
        List<PublicShortCardView> cards = new ArrayList<>(
                shortPage.getContent().stream()
                        .map(post -> toCard(
                                post,
                                shortPage.getNumber()))
                        .toList());
        boolean selectedPostLoaded = cards.stream()
                .anyMatch(card -> card.getPost().getPost().getId()
                        .equals(postId));
        if (!selectedPostLoaded) {
            cards.add(0, toCard(
                    shortPostSvc.getPublishedPost(postId),
                    shortPage.getNumber()));
        }

        PublicShortDetailPageView page = PublicShortDetailPageView.builder()
                .title(messageResolver.get("short.public.detail.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildDetailBreadcrumb())
                .feed(buildDetailFeed(
                        request,
                        postId,
                        shortPage,
                        cards,
                        query))
                .build();

        model.addAttribute(PublicShortDetailPageView.ATTRIBUTE, page);
        return "post/short/public/detail";
    }

    @GetMapping("/{postId}/stream")
    public String stream(
            @PathVariable UUID postId,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            PublicShortPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        query.setSize(SHORT_PAGE_DEFAULTS.getSize());
        Page<PublicShortPostResult> shortPage =
                shortPostSvc.getPublishedPosts(
                        filter,
                        query.toPageable(SHORT_PAGE_DEFAULTS));
        List<PublicShortCardView> cards = shortPage.getContent().stream()
                .map(post -> toCard(post, shortPage.getNumber()))
                .toList();

        model.addAttribute(
                PublicShortDetailFeedView.ATTRIBUTE,
                buildDetailFeed(
                        request,
                        postId,
                        shortPage,
                        cards,
                        query));
        return "post/short/public/fragments/detail-feed"
                + " :: items (feed=${feed})";
    }

    private PublicShortCardView toCard(
            PublicShortPostResult post,
            int pageNumber) {
        return PublicShortCardView.builder()
                .post(post)
                .detailPath(buildDetailPath(
                        post.getPost().getId(),
                        pageNumber))
                .build();
    }

    private PublicShortDetailFeedView buildDetailFeed(
            HttpServletRequest request,
            UUID activePostId,
            Page<PublicShortPostResult> shortPage,
            List<PublicShortCardView> cards,
            UiPageQuery query) {
        String nextPagePath = shortPage.hasNext()
                ? uiPaginationPathBuilder.build(
                        buildStreamPath(activePostId),
                        request,
                        query,
                        SHORT_PAGE_DEFAULTS)
                        .apply(shortPage.getNumber() + 1)
                : null;

        return PublicShortDetailFeedView.builder()
                .id(PUBLIC_SHORT_DETAIL_FEED_ID)
                .shorts(cards)
                .activePostId(activePostId)
                .nextPagePath(nextPagePath)
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

    private String buildDetailPath(
            UUID postId,
            int pageNumber) {
        return UriComponentsBuilder.fromPath(getShortsPath())
                .pathSegment(postId.toString())
                .queryParam("page", pageNumber)
                .queryParam("size", SHORT_PAGE_DEFAULTS.getSize())
                .build()
                .encode()
                .toUriString();
    }

    private String buildStreamPath(UUID postId) {
        return UriComponentsBuilder.fromPath(getShortsPath())
                .pathSegment(postId.toString(), "stream")
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
