package com.app.features.post.videopost.web.controller;

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
import com.app.core.enums.RecordStatus;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.media.entity.MediaEntity_;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.service.MediaService;
import com.app.features.media.web.support.MediaUploadComponentFactory;
import com.app.features.media.web.view.MediaUploadComponentView;
import com.app.features.post.videopost.schema.payload.CreateVideoPostPayload;
import com.app.features.post.videopost.schema.payload.CreateVideoSeriesPayload;
import com.app.features.post.videopost.schema.payload.AddVideoSeriesItemsPayload;
import com.app.features.post.videopost.schema.payload.UpdateVideoPostPayload;
import com.app.features.post.videopost.schema.payload.UpdateVideoSeriesPayload;
import com.app.features.post.videopost.schema.result.OwnerVideoPostResult;
import com.app.features.post.videopost.schema.result.VideoSeriesResult;
import com.app.features.post.videopost.service.VideoPostService;
import com.app.features.post.videopost.service.VideoSeriesItemService;
import com.app.features.post.videopost.service.VideoSeriesService;
import com.app.features.post.videopost.web.view.VideoPostComposerPageView;
import com.app.features.post.videopost.web.view.VideoSeriesComposerPageView;
import com.app.features.post.web.composer.support.PostComposerMediaViewFactory;
import com.app.features.post.web.composer.view.PostComposerMediaPickerView;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.view.UiBreadcrumbItemView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.enums.UiHtmxHistoryMode;
import com.app.features.ui.web.support.SocialShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.my-videos-path:/my/videos}")
public class VideoComposerPageController {

    private static final String VIDEO_PICKER_ID = "video-source-picker";
    private static final String COVER_PICKER_ID = "video-series-cover-picker";
    private static final String PICKER_FRAGMENT =
            "post/fragments/media-picker :: picker (picker=${picker})";
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
    private final VideoPostService videoPostSvc;
    private final VideoSeriesService videoSeriesSvc;
    private final VideoSeriesItemService videoSeriesItemSvc;
    private final MediaService mediaSvc;
    private final MediaUploadComponentFactory mediaUploadComponentFactory;
    private final PostComposerMediaViewFactory mediaViewFactory;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder paginationPathBuilder;

    @GetMapping("/create")
    @Secured(PermissionConstants.POST_CREATE)
    public String createVideo(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) UUID seriesId,
            HttpServletRequest request,
            @Valid @ModelAttribute("mediaFilter")
            MediaFilterCriteria mediaFilter,
            @Valid @ModelAttribute("mediaQuery") UiPageQuery mediaQuery,
            Model model) {
        model.addAttribute("form", new CreateVideoPostPayload());
        model.addAttribute("seriesId", seriesId);
        model.addAttribute(
                VideoPostComposerPageView.ATTRIBUTE,
                buildVideoPage(
                        currentUser,
                        request,
                        mediaFilter,
                        mediaQuery,
                        getCreateVideoPath(),
                        messageResolver.get("video.composer.title"),
                        messageResolver.get("video.composer.description"),
                        messageResolver.get("post.composer.submit")));
        return "post/video/owner/video-composer";
    }

    @PostMapping("/create")
    @Secured(PermissionConstants.POST_CREATE)
    public String submitCreateVideo(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) UUID seriesId,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute("form") CreateVideoPostPayload form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("seriesId", seriesId);
            model.addAttribute(
                    VideoPostComposerPageView.ATTRIBUTE,
                    buildVideoPage(
                            currentUser,
                            request,
                            new MediaFilterCriteria(),
                            new UiPageQuery(),
                            getCreateVideoPath(),
                            messageResolver.get("video.composer.title"),
                            messageResolver.get("video.composer.description"),
                            messageResolver.get("post.composer.submit")));
            return "post/video/owner/video-composer";
        }
        OwnerVideoPostResult video = videoPostSvc.createVideoPost(
                currentUser.getUserId(),
                form);
        if (seriesId != null) {
            AddVideoSeriesItemsPayload items =
                    new AddVideoSeriesItemsPayload();
            items.setVideoPostIds(List.of(video.getPost().getId()));
            videoSeriesItemSvc.addItems(
                    seriesId,
                    currentUser.getUserId(),
                    items);
            return HtmxRequestSupport.redirectView(
                    request,
                    response,
                    buildSeriesDetailPath(seriesId));
        }
        return HtmxRequestSupport.redirectView(
                request,
                response,
                buildVideoDetailPath(video.getPost().getId()));
    }

    @GetMapping("/{postId}/edit")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String editVideo(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            @Valid @ModelAttribute("mediaFilter")
            MediaFilterCriteria mediaFilter,
            @Valid @ModelAttribute("mediaQuery") UiPageQuery mediaQuery,
            Model model) {
        OwnerVideoPostResult video = videoPostSvc.getOwnerPost(
                postId,
                currentUser.getUserId());
        UpdateVideoPostPayload form = new UpdateVideoPostPayload();
        form.setTitle(video.getTitle());
        form.setDescription(video.getDescription());
        form.setSourceMediaId(video.getContent().getMedia().getId());
        model.addAttribute("form", form);
        model.addAttribute(
                VideoPostComposerPageView.ATTRIBUTE,
                buildVideoPage(
                        currentUser,
                        request,
                        mediaFilter,
                        mediaQuery,
                        buildVideoEditPath(postId),
                        messageResolver.get("video.composer.edit.title"),
                        messageResolver.get("video.composer.edit.description"),
                        messageResolver.get("post.composer.edit.submit")));
        return "post/video/owner/video-composer";
    }

    @PostMapping("/{postId}/edit")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String submitEditVideo(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute("form") UpdateVideoPostPayload form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    VideoPostComposerPageView.ATTRIBUTE,
                    buildVideoPage(
                            currentUser,
                            request,
                            new MediaFilterCriteria(),
                            new UiPageQuery(),
                            buildVideoEditPath(postId),
                            messageResolver.get("video.composer.edit.title"),
                            messageResolver.get("video.composer.edit.description"),
                            messageResolver.get("post.composer.edit.submit")));
            return "post/video/owner/video-composer";
        }
        videoPostSvc.updateOwnedVideoPost(
                postId,
                currentUser.getUserId(),
                form);
        return HtmxRequestSupport.redirectView(
                request,
                response,
                buildVideoDetailPath(postId));
    }

    @GetMapping({"/create/media", "/{postId}/edit/media"})
    public String videoMediaPicker(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable(required = false) UUID postId,
            HttpServletRequest request,
            @Valid @ModelAttribute("mediaFilter")
            MediaFilterCriteria mediaFilter,
            @Valid @ModelAttribute("mediaQuery") UiPageQuery mediaQuery,
            Model model) {
        String composerPath = postId == null
                ? getCreateVideoPath()
                : buildVideoEditPath(postId);
        if (postId != null) {
            videoPostSvc.getOwnerPost(postId, currentUser.getUserId());
        }
        model.addAttribute(
                PostComposerMediaPickerView.ATTRIBUTE,
                buildMediaPicker(
                        currentUser.getUserId(),
                        mediaFilter,
                        mediaQuery,
                        composerPath + "/media",
                        VIDEO_PICKER_ID,
                        MediaKind.VIDEO,
                        request));
        return PICKER_FRAGMENT;
    }

    @GetMapping("/series/create")
    @Secured(PermissionConstants.POST_CREATE)
    public String createSeries(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("mediaFilter")
            MediaFilterCriteria mediaFilter,
            @Valid @ModelAttribute("mediaQuery") UiPageQuery mediaQuery,
            Model model) {
        model.addAttribute("form", new CreateVideoSeriesPayload());
        model.addAttribute(
                VideoSeriesComposerPageView.ATTRIBUTE,
                buildSeriesPage(
                        currentUser,
                        request,
                        mediaFilter,
                        mediaQuery,
                        getCreateSeriesPath(),
                        messageResolver.get("video.series.composer.title"),
                        messageResolver.get("post.composer.submit")));
        return "post/video/owner/series-composer";
    }

    @PostMapping("/series/create")
    @Secured(PermissionConstants.POST_CREATE)
    public String submitCreateSeries(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute("form") CreateVideoSeriesPayload form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    VideoSeriesComposerPageView.ATTRIBUTE,
                    buildSeriesPage(
                            currentUser,
                            request,
                            new MediaFilterCriteria(),
                            new UiPageQuery(),
                            getCreateSeriesPath(),
                            messageResolver.get("video.series.composer.title"),
                            messageResolver.get("post.composer.submit")));
            return "post/video/owner/series-composer";
        }
        VideoSeriesResult series = videoSeriesSvc.createSeries(
                currentUser.getUserId(),
                form);
        return HtmxRequestSupport.redirectView(
                request,
                response,
                buildSeriesDetailPath(series.getId()));
    }

    @GetMapping("/series/{seriesId}/edit")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String editSeries(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            HttpServletRequest request,
            @Valid @ModelAttribute("mediaFilter")
            MediaFilterCriteria mediaFilter,
            @Valid @ModelAttribute("mediaQuery") UiPageQuery mediaQuery,
            Model model) {
        VideoSeriesResult series = videoSeriesSvc.getOwnedSeries(
                seriesId,
                currentUser.getUserId());
        UpdateVideoSeriesPayload form = new UpdateVideoSeriesPayload();
        form.setTitle(series.getTitle());
        form.setDescription(series.getDescription());
        if (series.getCoverMedia() != null) {
            form.setCoverMediaId(series.getCoverMedia().getId());
        }
        model.addAttribute("form", form);
        model.addAttribute(
                VideoSeriesComposerPageView.ATTRIBUTE,
                buildSeriesPage(
                        currentUser,
                        request,
                        mediaFilter,
                        mediaQuery,
                        buildSeriesEditPath(seriesId),
                        messageResolver.get("video.series.composer.edit.title"),
                        messageResolver.get("post.composer.edit.submit")));
        return "post/video/owner/series-composer";
    }

    @PostMapping("/series/{seriesId}/edit")
    @Secured(PermissionConstants.POST_UPDATE_OWN)
    public String submitEditSeries(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID seriesId,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute("form") UpdateVideoSeriesPayload form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    VideoSeriesComposerPageView.ATTRIBUTE,
                    buildSeriesPage(
                            currentUser,
                            request,
                            new MediaFilterCriteria(),
                            new UiPageQuery(),
                            buildSeriesEditPath(seriesId),
                            messageResolver.get("video.series.composer.edit.title"),
                            messageResolver.get("post.composer.edit.submit")));
            return "post/video/owner/series-composer";
        }
        videoSeriesSvc.updateOwnedSeries(
                seriesId,
                currentUser.getUserId(),
                form);
        return HtmxRequestSupport.redirectView(
                request,
                response,
                buildSeriesDetailPath(seriesId));
    }

    @GetMapping({
            "/series/create/media",
            "/series/{seriesId}/edit/media"
    })
    public String seriesCoverPicker(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable(required = false) UUID seriesId,
            HttpServletRequest request,
            @Valid @ModelAttribute("mediaFilter")
            MediaFilterCriteria mediaFilter,
            @Valid @ModelAttribute("mediaQuery") UiPageQuery mediaQuery,
            Model model) {
        String composerPath = seriesId == null
                ? getCreateSeriesPath()
                : buildSeriesEditPath(seriesId);
        if (seriesId != null) {
            videoSeriesSvc.getOwnedSeries(
                    seriesId,
                    currentUser.getUserId());
        }
        model.addAttribute(
                PostComposerMediaPickerView.ATTRIBUTE,
                buildMediaPicker(
                        currentUser.getUserId(),
                        mediaFilter,
                        mediaQuery,
                        composerPath + "/media",
                        COVER_PICKER_ID,
                        MediaKind.IMAGE,
                        request));
        return PICKER_FRAGMENT;
    }

    @GetMapping({
            "/create/upload-modal",
            "/{postId}/edit/upload-modal",
            "/series/create/upload-modal",
            "/series/{seriesId}/edit/upload-modal"
    })
    public String uploadModal(Model model) {
        model.addAttribute(
                MediaUploadComponentView.ATTRIBUTE,
                mediaUploadComponentFactory.buildLibraryUpload());
        return "media/fragments/upload-modal :: modal (upload=${upload})";
    }

    private VideoPostComposerPageView buildVideoPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            MediaFilterCriteria filter,
            UiPageQuery query,
            String actionPath,
            String title,
            String description,
            String submitLabel) {
        return VideoPostComposerPageView.builder()
                .title(title)
                .heading(title)
                .description(description)
                .submitLabel(submitLabel)
                .actionPath(actionPath)
                .backPath(getOwnerPath())
                .uploadPartialPath(actionPath + "/upload-modal")
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildBreadcrumb(title))
                .mediaPicker(buildMediaPicker(
                        currentUser.getUserId(),
                        filter,
                        query,
                        actionPath + "/media",
                        VIDEO_PICKER_ID,
                        MediaKind.VIDEO,
                        request))
                .build();
    }

    private VideoSeriesComposerPageView buildSeriesPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            MediaFilterCriteria filter,
            UiPageQuery query,
            String actionPath,
            String title,
            String submitLabel) {
        return VideoSeriesComposerPageView.builder()
                .title(title)
                .heading(title)
                .description(messageResolver.get(
                        "video.series.composer.description"))
                .submitLabel(submitLabel)
                .actionPath(actionPath)
                .backPath(getOwnerPath())
                .uploadPartialPath(actionPath + "/upload-modal")
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildBreadcrumb(title))
                .coverPicker(buildMediaPicker(
                        currentUser.getUserId(),
                        filter,
                        query,
                        actionPath + "/media",
                        COVER_PICKER_ID,
                        MediaKind.IMAGE,
                        request))
                .build();
    }

    private PostComposerMediaPickerView buildMediaPicker(
            UUID ownerId,
            MediaFilterCriteria filter,
            UiPageQuery query,
            String pickerPath,
            String pickerId,
            MediaKind kind,
            HttpServletRequest request) {
        MediaFilterCriteria criteria = new MediaFilterCriteria();
        criteria.setOriginalName(filter.getOriginalName());
        criteria.setKind(kind);
        criteria.setProcessingStatus(MediaProcessingStatus.READY);
        criteria.setStatus(RecordStatus.ACTIVE);
        Page<MediaResult> mediaPage = mediaSvc.getManyOwnedMedia(
                ownerId,
                criteria,
                query.toPageable(MEDIA_PAGE_DEFAULTS));
        IntFunction<String> pagePath = paginationPathBuilder.build(
                pickerPath,
                request,
                query,
                MEDIA_PAGE_DEFAULTS);
        UiPaginationView pagination = uiPaginationFactory.build(
                mediaPage,
                pagePath,
                UiHtmxNavigationView.builder()
                        .target("#" + pickerId)
                        .select("#" + pickerId)
                        .historyMode(UiHtmxHistoryMode.NONE)
                        .build());
        return PostComposerMediaPickerView.builder()
                .id(pickerId)
                .searchPath(pickerPath)
                .refreshPath(pickerPath)
                .originalName(filter.getOriginalName())
                .items(mediaPage.getContent().stream()
                        .map(media -> mediaViewFactory.toItem(media))
                        .toList())
                .pagination(pagination)
                .build();
    }

    private UiBreadcrumbView buildBreadcrumb(String title) {
        return UiBreadcrumbView.builder()
                .items(List.of(
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get("video.owner.title"))
                                .path(getOwnerPath())
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(title)
                                .active(true)
                                .build()))
                .build();
    }

    private String getCreateVideoPath() {
        return getOwnerPath() + "/create";
    }

    private String getCreateSeriesPath() {
        return getOwnerPath() + "/series/create";
    }

    private String buildVideoDetailPath(UUID postId) {
        return UriComponentsBuilder.fromPath(getOwnerPath())
                .pathSegment(postId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String buildVideoEditPath(UUID postId) {
        return buildVideoDetailPath(postId) + "/edit";
    }

    private String buildSeriesDetailPath(UUID seriesId) {
        return UriComponentsBuilder.fromPath(getOwnerPath())
                .pathSegment("series", seriesId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String buildSeriesEditPath(UUID seriesId) {
        return buildSeriesDetailPath(seriesId) + "/edit";
    }

    private String getOwnerPath() {
        return appProperties.getUi().getMyVideosPath();
    }
}
