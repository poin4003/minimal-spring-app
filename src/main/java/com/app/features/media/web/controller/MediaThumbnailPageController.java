package com.app.features.media.web.controller;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

import com.app.config.settings.AppProperties;
import com.app.config.security.web.HtmxRequestSupport;
import com.app.core.constant.PermissionConstants;
import com.app.core.enums.RecordStatus;
import com.app.core.exception.ExceptionFactory;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.media.schema.result.MediaDetailResult;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.service.MediaService;
import com.app.features.media.support.MediaProcessingPolicy;
import com.app.features.media.web.view.MediaThumbnailPageView;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.view.UiAssignmentActionView;
import com.app.features.ui.web.component.view.UiAssignmentPanelItemView;
import com.app.features.ui.web.component.view.UiAssignmentPanelView;
import com.app.features.ui.web.component.view.UiBreadcrumbItemView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiMetadataItemView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.support.UiShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/media/{mediaId}/thumbnail")
public class MediaThumbnailPageController {

    private static final String THUMBNAIL_PANEL_ID = "media-thumbnail-assignment-panel";

    private static final UiPageDefaults THUMBNAIL_PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(12)
            .sortBy("createdAt")
            .sortDirection(Sort.Direction.DESC)
            .build();

    private final AppProperties appProperties;
    private final UiShellFactory uiShellFactory;
    private final MediaService mediaSvc;
    private final MediaProcessingPolicy mediaProcessingPolicy;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;
    private final AppMessageResolver messageResolver;

    @GetMapping
    @Secured(PermissionConstants.MEDIA_MANAGE)
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @PathVariable UUID mediaId,
            @Valid @ModelAttribute("filter") MediaFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                MediaThumbnailPageView.ATTRIBUTE,
                buildPage(currentUser, request, mediaId, filter, query));
        return "media/thumbnail/index";
    }

    @PostMapping("/assign")
    @Secured(PermissionConstants.MEDIA_MANAGE)
    public String assign(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID mediaId,
            @RequestParam UUID targetId) {
        mediaSvc.updateThumbnail(
                mediaId,
                targetId);
        return HtmxRequestSupport.redirectView(
                request,
                response,
                getMediaListPath());
    }

    private MediaThumbnailPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID mediaId,
            MediaFilterCriteria filter,
            UiPageQuery query) {
        MediaDetailResult targetMedia = mediaSvc.getMediaDetail(mediaId);
        requireManualThumbnailSupport(targetMedia);

        MediaFilterCriteria thumbnailFilter = buildThumbnailFilter(filter);
        var imagePage = mediaSvc.getManyMedia(
                thumbnailFilter,
                query.toPageable(THUMBNAIL_PAGE_DEFAULTS));
        UiPaginationView pagination = uiPaginationFactory.build(
                imagePage,
                uiPaginationPathBuilder.build(
                        request,
                        query,
                        THUMBNAIL_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(THUMBNAIL_PANEL_ID));

        UiAssignmentPanelView assignmentPanel = UiAssignmentPanelView.builder()
                .id(THUMBNAIL_PANEL_ID)
                .title(messageResolver.get("media.thumbnail.readyImages"))
                .description(messageResolver.get(
                        "media.thumbnail.readyImagesDescription"))
                .emptyMessage(messageResolver.get(
                        "media.thumbnail.noReadyImages"))
                .rows(imagePage.getContent().stream()
                        .map(image -> toPanelItem(mediaId, image))
                        .toList())
                .pagination(pagination)
                .build();

        return MediaThumbnailPageView.builder()
                .title(messageResolver.get("media.thumbnail.select"))
                .listPath(getThumbnailPath(mediaId))
                .backPath(getMediaListPath())
                .uploadFallbackPath(getMediaListPath())
                .uploadPartialPath(
                        appProperties.getUi().getHomePath()
                                + "/media/uploads/thumbnail-modal")
                .shell(uiShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(UiBreadcrumbView.builder()
                        .items(List.of(
                                UiBreadcrumbItemView.builder()
                                        .label(messageResolver.get(
                                                "menu.mediaLibrary"))
                                        .path(getMediaListPath())
                                        .build(),
                                UiBreadcrumbItemView.builder()
                                        .label(messageResolver.get(
                                                "media.thumbnail.select"))
                                        .active(true)
                                        .build()))
                        .build())
                .filter(filter)
                .metadataItems(List.of(
                        UiMetadataItemView.builder()
                                .label(messageResolver.get("field.media"))
                                .value(targetMedia.getOriginalName())
                                .monospace(false)
                                .build(),
                        UiMetadataItemView.builder()
                                .label(messageResolver.get("field.kind"))
                                .value(targetMedia.getKind().name())
                                .monospace(true)
                                .build()))
                .assignmentPanel(assignmentPanel)
                .build();
    }

    private MediaFilterCriteria buildThumbnailFilter(MediaFilterCriteria filter) {
        MediaFilterCriteria criteria = new MediaFilterCriteria();
        criteria.setOriginalName(filter.getOriginalName());
        criteria.setKind(MediaKind.IMAGE);
        criteria.setProcessingStatus(MediaProcessingStatus.READY);
        criteria.setStatus(RecordStatus.ACTIVE);
        return criteria;
    }

    private UiAssignmentPanelItemView toPanelItem(
            UUID mediaId,
            MediaResult image) {
        return UiAssignmentPanelItemView.builder()
                .title(image.getOriginalName())
                .description(String.format(
                        Locale.ROOT,
                        "%s | %s",
                        formatFileSize(image.getFileSize()),
                        image.getCreatedAt()))
                .imageUrl(image.getThumbnailUrl())
                .action(UiAssignmentActionView.builder()
                        .path(getThumbnailPath(mediaId) + "/assign")
                        .label(messageResolver.get("media.thumbnail.use"))
                        .buttonClass("btn-outline-primary")
                        .targetId(image.getId().toString())
                        .build())
                .build();
    }

    private void requireManualThumbnailSupport(MediaDetailResult media) {
        if (!mediaProcessingPolicy.supportsManualThumbnail(media.getKind())) {
            throw ExceptionFactory.invalidParam(
                    "error.media.thumbnailKindUnsupported");
        }
        if (media.getStatus() != RecordStatus.ACTIVE
                || media.getProcessingStatus() != MediaProcessingStatus.READY) {
            throw ExceptionFactory.invalidParam(
                    "error.media.targetMustBeActiveReady");
        }
    }

    private String getThumbnailPath(UUID mediaId) {
        return getMediaListPath() + "/" + mediaId + "/thumbnail";
    }

    private String getMediaListPath() {
        return appProperties.getUi().getHomePath() + "/media";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1_024) {
            return bytes + " B";
        }

        String[] units = { "KB", "MB", "GB", "TB" };
        double value = bytes;
        int unitIndex = -1;
        while (value >= 1_024 && unitIndex < units.length - 1) {
            value /= 1_024;
            unitIndex++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex]);
    }
}
