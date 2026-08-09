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
import org.springframework.web.bind.annotation.PathVariable;
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
import com.app.features.post.schema.payload.UpdateStandardPostPayload;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.service.StandardPostService;
import com.app.features.post.standard.web.support.PostComposerMediaViewFactory;
import com.app.features.post.standard.web.view.PostComposerMediaPickerView;
import com.app.features.post.standard.web.view.StandardPostComposerModeView;
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
@RequestMapping("${app.ui.my-posts-path:/my/posts}")
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

    @GetMapping("/create")
    @Secured(PermissionConstants.POST_CREATE)
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
                        buildCreateMode(),
                        uiPaginationPathBuilder.build(
                                getMediaPickerPath(getCreatePath()),
                                request,
                                mediaQuery,
                                MEDIA_PAGE_DEFAULTS)));
        return "post/standard/owner/create";
    }

    @GetMapping("/create/media")
    @Secured(PermissionConstants.POST_CREATE)
    public String createMediaPicker(
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
                        getMediaPickerPath(getCreatePath()),
                        uiPaginationPathBuilder.build(
                                getMediaPickerPath(getCreatePath()),
                                request,
                                mediaQuery,
                                MEDIA_PAGE_DEFAULTS)));
        return "post/standard/owner/fragments/media-picker"
                + " :: picker (picker=${picker})";
    }

    @GetMapping("/create/upload-modal")
    @Secured(PermissionConstants.POST_CREATE)
    public String createUploadModal(Model model) {
        model.addAttribute(
                MediaUploadComponentView.ATTRIBUTE,
                mediaUploadComponentFactory.buildLibraryUpload());
        return "media/fragments/upload-modal :: modal (upload=${upload})";
    }

    @PostMapping("/create")
    @Secured(PermissionConstants.POST_CREATE)
    public String submitCreate(
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
                            buildCreateMode(),
                            buildDefaultMediaPagePath(
                                    mediaQuery,
                                    getMediaPickerPath(getCreatePath()))));
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

    @GetMapping("/{postId}/edit")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String edit(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            @Valid @ModelAttribute("mediaFilter")
            MediaFilterCriteria mediaFilter,
            @Valid @ModelAttribute("mediaQuery") UiPageQuery mediaQuery,
            Model model) {
        OwnerStandardPostResult post = standardPostSvc.getOwnerPost(
                postId,
                currentUser.getUserId());
        UpdateStandardPostPayload form = new UpdateStandardPostPayload();
        form.setContent(post.getContent());
        form.setMediaIds(post.getMedia().stream()
                .sorted((left, right) -> Integer.compare(
                        left.getPosition(),
                        right.getPosition()))
                .map(media -> media.getMedia().getId())
                .toList());

        String editPath = getEditPath(postId);
        model.addAttribute("form", form);
        model.addAttribute(
                StandardPostComposerPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        mediaFilter,
                        mediaQuery,
                        buildEditMode(postId),
                        uiPaginationPathBuilder.build(
                                getMediaPickerPath(editPath),
                                request,
                                mediaQuery,
                                MEDIA_PAGE_DEFAULTS)));
        return "post/standard/owner/create";
    }

    @GetMapping("/{postId}/edit/media")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String editMediaPicker(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            @Valid @ModelAttribute("mediaFilter")
            MediaFilterCriteria mediaFilter,
            @Valid @ModelAttribute("mediaQuery") UiPageQuery mediaQuery,
            Model model) {
        standardPostSvc.getOwnerPost(postId, currentUser.getUserId());
        String mediaPickerPath = getMediaPickerPath(getEditPath(postId));
        model.addAttribute(
                PostComposerMediaPickerView.ATTRIBUTE,
                buildMediaPicker(
                        currentUser.getUserId(),
                        mediaFilter,
                        mediaQuery,
                        mediaPickerPath,
                        uiPaginationPathBuilder.build(
                                mediaPickerPath,
                                request,
                                mediaQuery,
                                MEDIA_PAGE_DEFAULTS)));
        return "post/standard/owner/fragments/media-picker"
                + " :: picker (picker=${picker})";
    }

    @GetMapping("/{postId}/edit/upload-modal")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String editUploadModal(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            Model model) {
        standardPostSvc.getOwnerPost(postId, currentUser.getUserId());
        model.addAttribute(
                MediaUploadComponentView.ATTRIBUTE,
                mediaUploadComponentFactory.buildLibraryUpload());
        return "media/fragments/upload-modal :: modal (upload=${upload})";
    }

    @PostMapping("/{postId}/edit")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String submitEdit(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute("form") UpdateStandardPostPayload form,
            BindingResult bindingResult,
            Model model) {
        String editPath = getEditPath(postId);
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
                            buildEditMode(postId),
                            buildDefaultMediaPagePath(
                                    mediaQuery,
                                    getMediaPickerPath(editPath))));
            return HtmxRequestSupport.isHtmxRequest(request)
                    ? "post/standard/owner/fragments/composer"
                            + " :: composer (page=${page})"
                    : "post/standard/owner/create";
        }

        OwnerStandardPostResult post = standardPostSvc.updateOwnedStandardPost(
                postId,
                currentUser.getUserId(),
                form);
        return HtmxRequestSupport.redirectView(
                request,
                response,
                getDetailPath(post.getId()));
    }

    private StandardPostComposerPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            MediaFilterCriteria mediaFilter,
            UiPageQuery mediaQuery,
            StandardPostComposerModeView mode,
            IntFunction<String> mediaPagePath) {
        return StandardPostComposerPageView.builder()
                .title(mode.getTitle())
                .heading(mode.getTitle())
                .description(mode.getDescription())
                .submitLabel(mode.getSubmitLabel())
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(mode.getBreadcrumb())
                .actionPath(mode.getActionPath())
                .backPath(mode.getBackPath())
                .uploadPartialPath(mode.getActionPath() + "/upload-modal")
                .maxMediaCount(MAX_MEDIA_COUNT)
                .mediaPicker(buildMediaPicker(
                        currentUser.getUserId(),
                        mediaFilter,
                        mediaQuery,
                        getMediaPickerPath(mode.getActionPath()),
                        mediaPagePath))
                .build();
    }

    private StandardPostComposerModeView buildCreateMode() {
        return StandardPostComposerModeView.builder()
                .title(messageResolver.get("post.composer.title"))
                .description(messageResolver.get(
                        "post.composer.description"))
                .submitLabel(messageResolver.get("post.composer.submit"))
                .actionPath(getCreatePath())
                .backPath(getMyPostsPath())
                .breadcrumb(buildCreateBreadcrumb())
                .build();
    }

    private StandardPostComposerModeView buildEditMode(UUID postId) {
        return StandardPostComposerModeView.builder()
                .title(messageResolver.get("post.composer.edit.title"))
                .description(messageResolver.get(
                        "post.composer.edit.description"))
                .submitLabel(messageResolver.get(
                        "post.composer.edit.submit"))
                .actionPath(getEditPath(postId))
                .backPath(getDetailPath(postId))
                .breadcrumb(buildEditBreadcrumb(postId))
                .build();
    }

    private PostComposerMediaPickerView buildMediaPicker(
            UUID ownerId,
            MediaFilterCriteria mediaFilter,
            UiPageQuery mediaQuery,
            String mediaPickerPath,
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
                .searchPath(mediaPickerPath)
                .refreshPath(buildMediaRefreshPath(
                        mediaFilter,
                        mediaQuery,
                        mediaPickerPath))
                .originalName(mediaFilter.getOriginalName())
                .items(mediaPage.getContent().stream()
                        .map(media -> mediaViewFactory.toItem(media))
                        .toList())
                .pagination(pagination)
                .build();
    }

    private String buildMediaRefreshPath(
            MediaFilterCriteria mediaFilter,
            UiPageQuery mediaQuery,
            String mediaPickerPath) {
        UiPageQuery resolved = mediaQuery.applyDefaults(MEDIA_PAGE_DEFAULTS);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(
                        mediaPickerPath)
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
            UiPageQuery mediaQuery,
            String mediaPickerPath) {
        UiPageQuery resolved = mediaQuery.applyDefaults(MEDIA_PAGE_DEFAULTS);
        return page -> UriComponentsBuilder.fromPath(mediaPickerPath)
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

    private UiBreadcrumbView buildCreateBreadcrumb() {
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

    private UiBreadcrumbView buildEditBreadcrumb(UUID postId) {
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
                                .path(getDetailPath(postId))
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "post.composer.edit.title"))
                                .active(true)
                                .build()))
                .build();
    }

    private String getMediaPickerPath(String composerPath) {
        return composerPath + "/media";
    }

    private String getCreatePath() {
        return getMyPostsPath() + "/create";
    }

    private String getEditPath(UUID postId) {
        return getDetailPath(postId) + "/edit";
    }

    private String getDetailPath(UUID postId) {
        return UriComponentsBuilder.fromPath(getMyPostsPath())
                .pathSegment(postId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String getMyPostsPath() {
        return appProperties.getUi().getMyPostsPath();
    }
}
