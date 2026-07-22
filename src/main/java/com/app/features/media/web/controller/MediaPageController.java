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
import com.app.features.media.web.enums.MediaPreviewType;
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
        var mediaPage = mediaSvc.getManyMedia(
                filter,
                query.toPageable(MEDIA_PAGE_DEFAULTS));
        List<MediaGalleryItemView> items = mediaPage.getContent().stream()
                .map(media -> toGalleryItem(media, request))
                .toList();

        UiPaginationView pagination = uiPaginationFactory.build(
                mediaPage,
                uiPaginationPathBuilder.build(request, query, MEDIA_PAGE_DEFAULTS));
        MediaGalleryView mediaGallery = MediaGalleryView.builder()
                .title("Media Gallery")
                .description("Browse uploaded media and review its processing state.")
                .emptyMessage("No media found.")
                .items(items)
                .pagination(pagination)
                .build();

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
                .heading("Media Library")
                .description("Review and maintain media stored by the application.")
                .listPath(appProperties.getUi().getHomePath() + "/media")
                .shell(buildShell(currentUser, request))
                .filter(filter)
                .query(query.applyDefaults(MEDIA_PAGE_DEFAULTS))
                .kinds(Arrays.asList(MediaKind.values()))
                .processingStatuses(Arrays.asList(MediaProcessingStatus.values()))
                .statuses(Arrays.asList(RecordStatus.values()))
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

    private MediaGalleryItemView toGalleryItem(
            MediaResult media,
            HttpServletRequest request) {
        boolean imagePreviewAvailable = media.getKind() == MediaKind.IMAGE
                && media.getStatus() == RecordStatus.ACTIVE
                && media.getProcessingStatus() == MediaProcessingStatus.READY;
        String previewUrl = imagePreviewAvailable
                ? buildOriginalUrl(media.getPublicKey())
                : null;

        return MediaGalleryItemView.builder()
                .id(media.getId())
                .originalName(media.getOriginalName())
                .previewUrl(previewUrl)
                .kind(media.getKind())
                .processingStatus(media.getProcessingStatus())
                .status(media.getStatus())
                .createdByEmail(media.getCreatedBy() == null
                        ? null
                        : media.getCreatedBy().getEmail())
                .fileSizeLabel(formatFileSize(media.getFileSize()))
                .createdAt(media.getCreatedAt())
                .previewPath(buildSelectionPath(request, PREVIEW_MEDIA_ID, media.getId()))
                .metadataPath(buildSelectionPath(request, METADATA_MEDIA_ID, media.getId()))
                .detailPath(buildSelectionPath(request, DETAIL_MEDIA_ID, media.getId()))
                .retryPath(canRetry(media)
                        ? buildSelectionPath(request, RETRY_MEDIA_ID, media.getId())
                        : null)
                .deletePath(buildSelectionPath(request, DELETE_MEDIA_ID, media.getId()))
                .build();
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
        return UriComponentsBuilder.fromPath("/api/v1/public/media")
                .pathSegment(publicKey)
                .build()
                .encode()
                .toUriString();
    }

    private String buildHlsUrl(String publicKey) {
        return UriComponentsBuilder.fromPath("/api/v1/public/media")
                .pathSegment(publicKey, "hls", "index.m3u8")
                .build()
                .encode()
                .toUriString();
    }

    private boolean canRetry(MediaResult media) {
        return media.getProcessingStatus() == MediaProcessingStatus.FAILED
                && (media.getKind() == MediaKind.VIDEO || media.getKind() == MediaKind.AUDIO);
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
        UriComponentsBuilder builder = currentRequestBuilder(request);
        clearModalParameters(builder);
        return builder
                .replaceQueryParam(selectedParameter, mediaId)
                .build()
                .encode()
                .toUriString();
    }

    private String buildPostPath(
            HttpServletRequest request,
            UUID mediaId,
            String action) {
        UriComponentsBuilder builder = currentRequestBuilder(request)
                .replacePath(appProperties.getUi().getHomePath()
                        + "/media/"
                        + mediaId
                        + "/"
                        + action);
        clearModalParameters(builder);
        return builder.build().encode().toUriString();
    }

    private String buildReturnPath(HttpServletRequest request) {
        UriComponentsBuilder builder = currentRequestBuilder(request)
                .replacePath(appProperties.getUi().getHomePath() + "/media");
        clearModalParameters(builder);
        return builder.build().encode().toUriString();
    }

    private UriComponentsBuilder currentRequestBuilder(HttpServletRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.getRequestURI());
        request.getParameterMap().forEach((key, values) -> {
            for (String value : values) {
                builder.queryParam(key, value);
            }
        });
        return builder;
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
