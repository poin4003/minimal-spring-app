package com.app.features.post.standard.web.controller;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.security.web.HtmxRequestSupport;
import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.enums.RecordStatus;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.media.entity.MediaEntity_;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.service.MediaService;
import com.app.features.media.web.support.MediaUploadComponentFactory;
import com.app.features.media.web.view.MediaUploadComponentView;
import com.app.features.post.schema.payload.CreateStandardPostPayload;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.service.StandardPostService;
import com.app.features.post.standard.web.support.PostComposerMediaViewFactory;
import com.app.features.post.standard.web.view.PostComposerMediaPickerView;
import com.app.features.post.standard.web.view.StandardPostComposerPageView;
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
@RequestMapping("${app.ui.my-posts-path:/my/posts}/create")
@Secured(PermissionConstants.POST_CREATE)
public class StandardPostComposerPageController {

    private static final int MAX_MEDIA_COUNT = 20;
    private static final String MEDIA_PICKER_ID = "post-media-picker";

    private static final UiPageDefaults MEDIA_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(12)
                    .sortBy(MediaEntity_.CREATED_AT)
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final SocialShellFactory socialShellFactory;
    private final StandardPostService standardPostSvc;
    private final MediaService mediaSvc;
    private final MediaUploadComponentFactory mediaUploadComponentFactory;
    private final PostComposerMediaViewFactory mediaViewFactory;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;

    @GetMapping
    public String create(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("mediaFilter")
            MediaFilterCriteria mediaFilter,
            @Valid @ModelAttribute("mediaQuery") UiPageQuery mediaQuery,
            Model model) {
        CreateStandardPostPayload form = new CreateStandardPostPayload();
        model.addAttribute("form", form);
        model.addAttribute(
                StandardPostComposerPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        mediaFilter,
                        mediaQuery,
                        uiPaginationPathBuilder.build(
                                getMediaPickerPath(),
                                request,
                                mediaQuery,
                                MEDIA_PAGE_DEFAULTS)));
        return "post/standard/owner/create";
    }

    @GetMapping("/media")
    public String mediaPicker(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("mediaFilter")
            MediaFilterCriteria mediaFilter,
            @Valid @ModelAttribute("mediaQuery") UiPageQuery mediaQuery,
            Model model) {
        model.addAttribute(
                PostComposerMediaPickerView.ATTRIBUTE,
                buildMediaPicker(
                        currentUser.getUserId(),
                        mediaFilter,
                        mediaQuery,
                        uiPaginationPathBuilder.build(
                                getMediaPickerPath(),
                                request,
                                mediaQuery,
                                MEDIA_PAGE_DEFAULTS)));
        return "post/standard/owner/fragments/media-picker"
                + " :: picker (picker=${picker})";
    }

    @GetMapping("/upload-modal")
    public String uploadModal(Model model) {
        model.addAttribute(
                MediaUploadComponentView.ATTRIBUTE,
                mediaUploadComponentFactory.buildLibraryUpload());
        return "media/fragments/upload-modal :: modal (upload=${upload})";
    }

    @PostMapping
    public String submit(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute("form") CreateStandardPostPayload form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            MediaFilterCriteria mediaFilter = new MediaFilterCriteria();
            UiPageQuery mediaQuery = new UiPageQuery();
            model.addAttribute(
                    StandardPostComposerPageView.ATTRIBUTE,
                    buildPage(
                            currentUser,
                            request,
                            mediaFilter,
                            mediaQuery,
                            buildDefaultMediaPagePath(mediaQuery)));
            return HtmxRequestSupport.isHtmxRequest(request)
                    ? "post/standard/owner/fragments/composer"
                            + " :: composer (page=${page})"
                    : "post/standard/owner/create";
        }

        OwnerStandardPostResult post = standardPostSvc.createStandardPost(
                currentUser.getUserId(),
                form);
        return HtmxRequestSupport.redirectView(
                request,
                response,
                getMyPostsPath() + "/" + post.getId());
    }

    private StandardPostComposerPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            MediaFilterCriteria mediaFilter,
            UiPageQuery mediaQuery,
            IntFunction<String> mediaPagePath) {
        return StandardPostComposerPageView.builder()
                .title(messageResolver.get("post.composer.title"))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildBreadcrumb())
                .actionPath(getCreatePath())
                .backPath(getMyPostsPath())
                .uploadPartialPath(getCreatePath() + "/upload-modal")
                .maxMediaCount(MAX_MEDIA_COUNT)
                .mediaPicker(buildMediaPicker(
                        currentUser.getUserId(),
                        mediaFilter,
                        mediaQuery,
                        mediaPagePath))
                .build();
    }

    private PostComposerMediaPickerView buildMediaPicker(
            UUID ownerId,
            MediaFilterCriteria mediaFilter,
            UiPageQuery mediaQuery,
            IntFunction<String> mediaPagePath) {
        MediaFilterCriteria criteria = buildReadyMediaFilter(mediaFilter);
        Page<MediaResult> mediaPage = mediaSvc.getManyOwnedMedia(
                ownerId,
                criteria,
                mediaQuery.toPageable(MEDIA_PAGE_DEFAULTS));
        UiPaginationView pagination = uiPaginationFactory.build(
                mediaPage,
                mediaPagePath,
                UiHtmxNavigationView.builder()
                        .target("#" + MEDIA_PICKER_ID)
                        .select("#" + MEDIA_PICKER_ID)
                        .pushUrl(false)
                        .build());

        return PostComposerMediaPickerView.builder()
                .id(MEDIA_PICKER_ID)
                .searchPath(getMediaPickerPath())
                .refreshPath(buildMediaRefreshPath(
                        mediaFilter,
                        mediaQuery))
                .originalName(mediaFilter.getOriginalName())
                .items(mediaPage.getContent().stream()
                        .map(media -> mediaViewFactory.toItem(media))
                        .toList())
                .pagination(pagination)
                .build();
    }

    private String buildMediaRefreshPath(
            MediaFilterCriteria mediaFilter,
            UiPageQuery mediaQuery) {
        UiPageQuery resolved = mediaQuery.applyDefaults(MEDIA_PAGE_DEFAULTS);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(
                        getMediaPickerPath())
                .queryParam("page", resolved.getPage())
                .queryParam("size", resolved.getSize())
                .queryParam("sortBy", resolved.getSortBy())
                .queryParam(
                        "sortDirection",
                        resolved.getSortDirection().name());
        if (mediaFilter.hasOriginalName()) {
            builder.queryParam(
                    "originalName",
                    mediaFilter.getOriginalName());
        }
        return builder.build().encode().toUriString();
    }

    private MediaFilterCriteria buildReadyMediaFilter(
            MediaFilterCriteria mediaFilter) {
        MediaFilterCriteria criteria = new MediaFilterCriteria();
        criteria.setOriginalName(mediaFilter.getOriginalName());
        criteria.setProcessingStatus(MediaProcessingStatus.READY);
        criteria.setStatus(RecordStatus.ACTIVE);
        return criteria;
    }

    private IntFunction<String> buildDefaultMediaPagePath(
            UiPageQuery mediaQuery) {
        UiPageQuery resolved = mediaQuery.applyDefaults(MEDIA_PAGE_DEFAULTS);
        return page -> UriComponentsBuilder.fromPath(getMediaPickerPath())
                .queryParam("page", page)
                .queryParam("size", resolved.getSize())
                .queryParam("sortBy", resolved.getSortBy())
                .queryParam(
                        "sortDirection",
                        resolved.getSortDirection().name())
                .build()
                .encode()
                .toUriString();
    }

    private UiBreadcrumbView buildBreadcrumb() {
        return UiBreadcrumbView.builder()
                .items(List.of(
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "post.owner.list.title"))
                                .path(getMyPostsPath())
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "post.composer.title"))
                                .active(true)
                                .build()))
                .build();
    }

    private String getMediaPickerPath() {
        return getCreatePath() + "/media";
    }

    private String getCreatePath() {
        return getMyPostsPath() + "/create";
    }

    private String getMyPostsPath() {
        return appProperties.getUi().getMyPostsPath();
    }
}
