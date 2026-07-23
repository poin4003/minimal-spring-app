package com.app.features.media.web.controller;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.enums.RecordStatus;
import com.app.core.menu.MenuService;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.media.schema.result.MediaDetailResult;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.schema.result.MediaVariantResult;
import com.app.features.media.service.MediaService;
import com.app.features.media.support.MediaProcessingPolicy;
import com.app.features.media.web.enums.MediaPreviewType;
import com.app.features.media.web.support.MediaUploadComponentFactory;
import com.app.features.media.web.view.MediaGalleryItemView;
import com.app.features.media.web.view.MediaGalleryView;
import com.app.features.media.web.view.MediaListPageView;
import com.app.features.media.web.view.MediaPreviewModalView;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.view.UiConfirmModalView;
import com.app.features.ui.web.component.view.UiDetailItemView;
import com.app.features.ui.web.component.view.UiDetailModalView;
import com.app.features.ui.web.component.view.UiMetadataItemView;
import com.app.features.ui.web.component.view.UiMetadataModalView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.view.UiCurrentUserView;
import com.app.features.ui.web.view.UiShellView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/media")
public class MediaPageController {

    private static final String PREVIEW_MEDIA_ID = "previewMediaId";
    private static final String METADATA_MEDIA_ID = "metadataMediaId";
    private static final String DETAIL_MEDIA_ID = "detailMediaId";
    private static final String DELETE_MEDIA_ID = "deleteMediaId";
    private static final String RETRY_MEDIA_ID = "retryMediaId";

    private static final UiPageDefaults MEDIA_PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(12)
            .sortBy("createdAt")
            .sortDirection(Sort.Direction.DESC)
            .build();

    private final AppProperties appProperties;
    private final MenuService menuSvc;
    private final MediaService mediaSvc;
    private final MediaProcessingPolicy mediaProcessingPolicy;
    private final MediaUploadComponentFactory mediaUploadComponentFactory;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;

    @GetMapping
    @Secured(PermissionConstants.MEDIA_VIEW)
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter") MediaFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @RequestParam(required = false) UUID previewMediaId,
            @RequestParam(required = false) UUID metadataMediaId,
            @RequestParam(required = false) UUID detailMediaId,
            @RequestParam(required = false) UUID deleteMediaId,
            @RequestParam(required = false) UUID retryMediaId,
            Model model) {
        model.addAttribute(
                MediaListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        filter,
                        query,
                        previewMediaId,
                        metadataMediaId,
                        detailMediaId,
                        deleteMediaId,
                        retryMediaId));
        return "media/index";
    }

    @GetMapping("/gallery")
    @Secured(PermissionConstants.MEDIA_VIEW)
    public String gallery(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter") MediaFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                MediaGalleryView.ATTRIBUTE,
                buildMediaGallery(
                        filter,
                        query,
                        request,
                        currentUser.getUserId()));
        return "media/fragments/gallery :: gallery (gallery=${gallery})";
    }

    @GetMapping("/{mediaId}/preview")
    @Secured(PermissionConstants.MEDIA_VIEW)
    public String preview(
            @PathVariable UUID mediaId,
            Model model) {
        model.addAttribute(
                MediaPreviewModalView.ATTRIBUTE,
                buildPreviewModal(mediaId));
        return "media/fragments/preview-modal :: modal (modal=${modal})";
    }

    @GetMapping("/{mediaId}/metadata")
    @Secured(PermissionConstants.MEDIA_VIEW)
    public String metadata(
            @PathVariable UUID mediaId,
            Model model) {
        model.addAttribute(
                UiMetadataModalView.ATTRIBUTE,
                buildMetadataModal(mediaId));
        return "fragments/components/metadata-modal :: modal (modal=${modal})";
    }

    @GetMapping("/{mediaId}/detail")
    @Secured(PermissionConstants.MEDIA_VIEW)
    public String detail(
            @PathVariable UUID mediaId,
            Model model) {
        model.addAttribute(
                UiDetailModalView.ATTRIBUTE,
                buildDetailModal(mediaId));
        return "fragments/components/detail-modal :: modal (modal=${modal})";
    }

    @PostMapping("/{mediaId}/delete")
    @Secured(PermissionConstants.MEDIA_MANAGE)
    public String delete(
            @PathVariable UUID mediaId,
            HttpServletRequest request) {
        mediaSvc.deleteMedia(mediaId);
        return "redirect:" + buildReturnPath(request);
    }

    @PostMapping("/{mediaId}/retry")
    @Secured(PermissionConstants.MEDIA_MANAGE)
    public String retry(
            @PathVariable UUID mediaId,
            HttpServletRequest request) {
        mediaSvc.retryProcessing(mediaId);
        return "redirect:" + buildReturnPath(request);
    }

    private MediaListPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            MediaFilterCriteria filter,
            UiPageQuery query,
            UUID previewMediaId,
            UUID metadataMediaId,
            UUID detailMediaId,
            UUID deleteMediaId,
            UUID retryMediaId) {
        MediaGalleryView mediaGallery = buildMediaGallery(
                filter,
                query,
                request,
                currentUser.getUserId());

        MediaPreviewModalView previewModal = previewMediaId == null
                ? null
                : buildPreviewModal(previewMediaId);
        UiMetadataModalView metadataModal = metadataMediaId == null
                ? null
                : buildMetadataModal(metadataMediaId);
        UiDetailModalView detailModal = detailMediaId == null
                ? null
                : buildDetailModal(detailMediaId);
        UiConfirmModalView deleteModal = deleteMediaId == null
                ? null
                : buildDeleteModal(deleteMediaId, request);
        UiConfirmModalView retryModal = retryMediaId == null
                ? null
                : buildRetryModal(retryMediaId, request);

        return MediaListPageView.builder()
                .title("Media Library")
                .listPath(getMediaListPath())
                .shell(buildShell(currentUser, request))
                .filter(filter)
                .query(query.applyDefaults(MEDIA_PAGE_DEFAULTS))
                .kinds(Arrays.asList(MediaKind.values()))
                .processingStatuses(Arrays.asList(MediaProcessingStatus.values()))
                .statuses(Arrays.asList(RecordStatus.values()))
                .uploadComponent(mediaUploadComponentFactory.buildLibraryUpload())
                .mediaGallery(mediaGallery)
                .previewModal(previewModal)
                .metadataModal(metadataModal)
                .detailModal(detailModal)
                .deleteModal(deleteModal)
                .retryModal(retryModal)
                .openPreviewModal(previewModal != null)
                .openMetadataModal(metadataModal != null)
                .openDetailModal(detailModal != null)
                .openDeleteModal(deleteModal != null)
                .openRetryModal(retryModal != null)
                .build();
    }

    private MediaGalleryView buildMediaGallery(
            MediaFilterCriteria filter,
            UiPageQuery query,
            HttpServletRequest request,
            UUID currentUserId) {
        var mediaPage = mediaSvc.getManyMedia(
                filter,
                query.toPageable(MEDIA_PAGE_DEFAULTS));
        List<MediaGalleryItemView> items = mediaPage.getContent().stream()
                .map(media -> toGalleryItem(media, request, currentUserId))
                .toList();

        UiPaginationView pagination = uiPaginationFactory.build(
                mediaPage,
                uiPaginationPathBuilder.build(
                        getMediaListPath(),
                        request,
                        query,
                        MEDIA_PAGE_DEFAULTS));
        return MediaGalleryView.builder()
                .title("Media Gallery")
                .description("Browse uploaded media and review its processing state.")
                .emptyMessage("No media found.")
                .refreshPath(buildGalleryRefreshPath(request))
                .items(items)
                .pagination(pagination)
                .build();
    }

    private MediaGalleryItemView toGalleryItem(
            MediaResult media,
            HttpServletRequest request,
            UUID currentUserId) {
        return MediaGalleryItemView.builder()
                .id(media.getId())
                .originalName(media.getOriginalName())
                .thumbnailUrl(media.getThumbnailUrl())
                .kind(media.getKind())
                .processingStatus(media.getProcessingStatus())
                .status(media.getStatus())
                .createdByEmail(media.getCreatedBy() == null
                        ? null
                        : media.getCreatedBy().getEmail())
                .fileSizeLabel(formatFileSize(media.getFileSize()))
                .createdAt(media.getCreatedAt())
                .previewPath(buildSelectionPath(request, PREVIEW_MEDIA_ID, media.getId()))
                .previewPartialPath(buildPartialPath(media.getId(), "preview"))
                .metadataPath(buildSelectionPath(request, METADATA_MEDIA_ID, media.getId()))
                .metadataPartialPath(buildPartialPath(media.getId(), "metadata"))
                .detailPath(buildSelectionPath(request, DETAIL_MEDIA_ID, media.getId()))
                .detailPartialPath(buildPartialPath(media.getId(), "detail"))
                .thumbnailSelectionPath(canSelectThumbnail(media, currentUserId)
                        ? getMediaListPath() + "/" + media.getId() + "/thumbnail"
                        : null)
                .retryPath(canRetry(media)
                        ? buildSelectionPath(request, RETRY_MEDIA_ID, media.getId())
                        : null)
                .deletePath(buildSelectionPath(request, DELETE_MEDIA_ID, media.getId()))
                .build();
    }

    private boolean canSelectThumbnail(
            MediaResult media,
            UUID currentUserId) {
        return media.getCreatedBy() != null
                && currentUserId.equals(media.getCreatedBy().getId())
                && media.getStatus() == RecordStatus.ACTIVE
                && media.getProcessingStatus() == MediaProcessingStatus.READY
                && mediaProcessingPolicy.supportsManualThumbnail(media.getKind());
    }

    private MediaPreviewModalView buildPreviewModal(UUID mediaId) {
        MediaDetailResult media = mediaSvc.getMediaDetail(mediaId);
        boolean deliveryAvailable = media.getStatus() == RecordStatus.ACTIVE
                && media.getProcessingStatus() == MediaProcessingStatus.READY;

        MediaPreviewType previewType = deliveryAvailable
                ? resolvePreviewType(media)
                : MediaPreviewType.UNAVAILABLE;
        String originalUrl = deliveryAvailable
                ? buildOriginalUrl(media.getPublicKey())
                : null;
        String sourceUrl = switch (previewType) {
            case VIDEO, AUDIO -> buildHlsUrl(media.getPublicKey());
            case IMAGE, PDF, DOWNLOAD -> originalUrl;
            case UNAVAILABLE -> null;
        };

        return MediaPreviewModalView.builder()
                .mediaId(media.getId())
                .id("media-preview-modal")
                .title("Media Preview")
                .description(String.format(
                        Locale.ROOT,
                        "%s | %s | %s",
                        media.getOriginalName(),
                        media.getKind(),
                        formatFileSize(media.getFileSize())))
                .previewType(previewType)
                .sourceUrl(sourceUrl)
                .originalUrl(originalUrl)
                .unavailableMessage(deliveryAvailable
                        ? "This file type cannot be previewed in the browser."
                        : buildUnavailableMessage(media))
                .build();
    }

    private MediaPreviewType resolvePreviewType(MediaDetailResult media) {
        return switch (media.getKind()) {
            case IMAGE -> MediaPreviewType.IMAGE;
            case VIDEO -> MediaPreviewType.VIDEO;
            case AUDIO -> MediaPreviewType.AUDIO;
            case DOCUMENT -> "application/pdf".equalsIgnoreCase(media.getContentType())
                    ? MediaPreviewType.PDF
                    : MediaPreviewType.DOWNLOAD;
            case FILE -> MediaPreviewType.DOWNLOAD;
        };
    }

    private String buildUnavailableMessage(MediaDetailResult media) {
        if (media.getStatus() != RecordStatus.ACTIVE) {
            return "Preview is unavailable because this media is inactive.";
        }
        if (media.getProcessingStatus() == MediaProcessingStatus.FAILED) {
            return "Preview is unavailable because media processing failed.";
        }
        return "Preview will be available after media processing is complete.";
    }

    private String buildOriginalUrl(String publicKey) {
        return UriComponentsBuilder.fromPath(appProperties.getMedia().getPublicPath())
                .pathSegment(publicKey)
                .build()
                .encode()
                .toUriString();
    }

    private String buildHlsUrl(String publicKey) {
        return UriComponentsBuilder.fromPath(appProperties.getMedia().getPublicPath())
                .pathSegment(publicKey, "hls", "index.m3u8")
                .build()
                .encode()
                .toUriString();
    }

    private boolean canRetry(MediaResult media) {
        return media.getProcessingStatus() == MediaProcessingStatus.FAILED
                && mediaProcessingPolicy.requiresProcessing(
                        media.getKind(),
                        media.getContentType());
    }

    private UiMetadataModalView buildMetadataModal(UUID mediaId) {
        MediaDetailResult media = mediaSvc.getMediaDetail(mediaId);

        return UiMetadataModalView.builder()
                .id("media-metadata-modal")
                .title("Media Metadata")
                .items(List.of(
                        metadataItem("Media Id", String.valueOf(media.getId()), true),
                        metadataItem("Public Key", media.getPublicKey(), true),
                        metadataItem("Original Name", media.getOriginalName(), false),
                        metadataItem("Content Type", media.getContentType(), true),
                        metadataItem(
                                "Thumbnail URL",
                                media.getThumbnailUrl(),
                                true),
                        metadataItem(
                                "Created By Id",
                                media.getCreatedBy() == null
                                        ? null
                                        : String.valueOf(media.getCreatedBy().getId()),
                                true),
                        metadataItem(
                                "Created By Email",
                                media.getCreatedBy() == null
                                        ? null
                                        : media.getCreatedBy().getEmail(),
                                false),
                        metadataItem("Created At", media.getCreatedAt(), true),
                        metadataItem("Updated At", media.getUpdatedAt(), true)))
                .build();
    }

    private UiDetailModalView buildDetailModal(UUID mediaId) {
        MediaDetailResult media = mediaSvc.getMediaDetail(mediaId);
        List<UiDetailItemView> variants = media.getVariants().stream()
                .map(variant -> toVariantItem(variant))
                .toList();

        return UiDetailModalView.builder()
                .id("media-detail-modal")
                .title("Media Detail")
                .description(String.format(
                        Locale.ROOT,
                        "%s | %s | %s | %s",
                        media.getOriginalName(),
                        media.getKind(),
                        media.getProcessingStatus(),
                        formatFileSize(media.getFileSize())))
                .listTitle("Generated Variants")
                .items(variants)
                .emptyMessage("This media uses the original file directly and has no generated variants.")
                .build();
    }

    private UiDetailItemView toVariantItem(MediaVariantResult variant) {
        List<String> details = new ArrayList<>();
        if (variant.getContentType() != null) {
            details.add(variant.getContentType());
        }
        if (variant.getWidth() != null && variant.getHeight() != null) {
            details.add(variant.getWidth() + "x" + variant.getHeight());
        }
        if (variant.getBitrate() != null) {
            details.add(variant.getBitrate() / 1_000 + " kbps");
        }

        String title = variant.getVariantType().name().replace('_', ' ');
        if (variant.getVariantKey() != null && !variant.getVariantKey().isBlank()) {
            title += " | " + variant.getVariantKey();
        }

        return UiDetailItemView.builder()
                .title(title)
                .description(details.isEmpty() ? null : String.join(" | ", details))
                .build();
    }

    private UiConfirmModalView buildDeleteModal(
            UUID mediaId,
            HttpServletRequest request) {
        MediaDetailResult media = mediaSvc.getMediaDetail(mediaId);
        return UiConfirmModalView.builder()
                .id("media-delete-modal")
                .title("Delete Media")
                .description("Delete '" + media.getOriginalName()
                        + "' and all of its stored variants. This action cannot be undone.")
                .actionPath(buildPostPath(request, mediaId, "delete"))
                .confirmLabel("Delete Media")
                .confirmButtonClass("btn-danger")
                .build();
    }

    private UiConfirmModalView buildRetryModal(
            UUID mediaId,
            HttpServletRequest request) {
        MediaDetailResult media = mediaSvc.getMediaDetail(mediaId);
        return UiConfirmModalView.builder()
                .id("media-retry-modal")
                .title("Retry Media Processing")
                .description("Queue '" + media.getOriginalName() + "' for processing again.")
                .actionPath(buildPostPath(request, mediaId, "retry"))
                .confirmLabel("Retry Processing")
                .confirmButtonClass("btn-warning")
                .build();
    }

    private UiMetadataItemView metadataItem(
            String label,
            String value,
            boolean monospace) {
        return UiMetadataItemView.builder()
                .label(label)
                .value(value == null ? "-" : value)
                .monospace(monospace)
                .build();
    }

    private UiShellView buildShell(
            UserPrincipal currentUser,
            HttpServletRequest request) {
        return UiShellView.builder()
                .title(appProperties.getUi().getApplicationTitle())
                .logoutPath(appProperties.getUi().getLogoutPath())
                .currentUser(UiCurrentUserView.builder()
                        .email(currentUser.getEmail())
                        .authorities(currentUser.getAuthorities().stream()
                                .map(authority -> authority.getAuthority())
                                .toList())
                        .build())
                .menuTree(menuSvc.getMenuTree(request.getRequestURI()))
                .build();
    }

    private String buildSelectionPath(
            HttpServletRequest request,
            String selectedParameter,
            UUID mediaId) {
        UriComponentsBuilder builder = requestQueryBuilder(
                getMediaListPath(),
                request);
        clearModalParameters(builder);
        return builder
                .replaceQueryParam(selectedParameter, mediaId)
                .build()
                .encode()
                .toUriString();
    }

    private String buildPartialPath(UUID mediaId, String fragmentName) {
        return UriComponentsBuilder
                .fromPath(appProperties.getUi().getHomePath() + "/media")
                .pathSegment(mediaId.toString(), fragmentName)
                .build()
                .encode()
                .toUriString();
    }

    private String buildPostPath(
            HttpServletRequest request,
            UUID mediaId,
            String action) {
        UriComponentsBuilder builder = requestQueryBuilder(
                getMediaListPath(),
                request)
                .replacePath(appProperties.getUi().getHomePath()
                        + "/media/"
                        + mediaId
                        + "/"
                        + action);
        clearModalParameters(builder);
        return builder.build().encode().toUriString();
    }

    private String buildReturnPath(HttpServletRequest request) {
        UriComponentsBuilder builder = requestQueryBuilder(
                getMediaListPath(),
                request);
        clearModalParameters(builder);
        return builder.build().encode().toUriString();
    }

    private String buildGalleryRefreshPath(HttpServletRequest request) {
        UriComponentsBuilder builder = requestQueryBuilder(
                getMediaListPath() + "/gallery",
                request);
        clearModalParameters(builder);
        return builder.build().encode().toUriString();
    }

    private UriComponentsBuilder requestQueryBuilder(
            String path,
            HttpServletRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        request.getParameterMap().forEach((key, values) -> {
            for (String value : values) {
                builder.queryParam(key, value);
            }
        });
        return builder;
    }

    private String getMediaListPath() {
        return appProperties.getUi().getHomePath() + "/media";
    }

    private void clearModalParameters(UriComponentsBuilder builder) {
        builder.replaceQueryParam(PREVIEW_MEDIA_ID)
                .replaceQueryParam(METADATA_MEDIA_ID)
                .replaceQueryParam(DETAIL_MEDIA_ID)
                .replaceQueryParam(DELETE_MEDIA_ID)
                .replaceQueryParam(RETRY_MEDIA_ID);
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
