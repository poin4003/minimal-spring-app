package com.app.features.user.web.controller;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.config.security.web.HtmxRequestSupport;
import com.app.config.settings.AppProperties;
import com.app.core.enums.RecordStatus;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.service.MediaService;
import com.app.features.media.web.support.MediaUploadComponentFactory;
import com.app.features.media.web.view.MediaUploadComponentView;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.view.UiAssignmentActionView;
import com.app.features.ui.web.component.view.UiAssignmentPanelItemView;
import com.app.features.ui.web.component.view.UiAssignmentPanelView;
import com.app.features.ui.web.component.view.UiBreadcrumbItemView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.support.UiShellFactory;
import com.app.features.user.service.ProfileService;
import com.app.features.user.web.view.ProfileAvatarPageView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/profile/avatar")
public class ProfileAvatarPageController {

    private static final String AVATAR_PANEL_ID =
            "profile-avatar-assignment-panel";

    private static final UiPageDefaults AVATAR_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(12)
                    .sortBy("createdAt")
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private final AppProperties appProperties;
    private final UiShellFactory uiShellFactory;
    private final ProfileService profileSvc;
    private final MediaService mediaSvc;
    private final MediaUploadComponentFactory mediaUploadComponentFactory;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter") MediaFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                ProfileAvatarPageView.ATTRIBUTE,
                buildPage(currentUser, request, filter, query));
        return "profile/avatar/index";
    }

    @GetMapping("/upload-modal")
    public String uploadModal(Model model) {
        model.addAttribute(
                MediaUploadComponentView.ATTRIBUTE,
                mediaUploadComponentFactory.buildProfileAvatarUpload());
        return "media/fragments/upload-modal :: modal (upload=${upload})";
    }

    @PostMapping
    public String assign(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam UUID targetId) {
        profileSvc.updateAvatar(currentUser.getUserId(), targetId);
        return HtmxRequestSupport.redirectView(
                request,
                response,
                getProfilePath());
    }

    private ProfileAvatarPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            MediaFilterCriteria filter,
            UiPageQuery query) {
        MediaFilterCriteria avatarFilter = buildAvatarFilter(filter);
        var imagePage = mediaSvc.getManyOwnedMedia(
                currentUser.getUserId(),
                avatarFilter,
                query.toPageable(AVATAR_PAGE_DEFAULTS));
        UiPaginationView pagination = uiPaginationFactory.build(
                imagePage,
                uiPaginationPathBuilder.build(
                        request,
                        query,
                        AVATAR_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(AVATAR_PANEL_ID));

        UiAssignmentPanelView assignmentPanel = UiAssignmentPanelView.builder()
                .id(AVATAR_PANEL_ID)
                .title("Ready Images")
                .description("Choose a ready image from your media library.")
                .emptyMessage("No ready images are available.")
                .rows(imagePage.getContent().stream()
                        .map(image -> toPanelItem(image))
                        .toList())
                .pagination(pagination)
                .build();

        return ProfileAvatarPageView.builder()
                .title("Select Avatar")
                .listPath(getAvatarPath())
                .backPath(getProfilePath())
                .uploadPartialPath(getAvatarPath() + "/upload-modal")
                .shell(uiShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(UiBreadcrumbView.builder()
                        .items(List.of(
                                UiBreadcrumbItemView.builder()
                                        .label("Profile Settings")
                                        .path(getProfilePath())
                                        .build(),
                                UiBreadcrumbItemView.builder()
                                        .label("Select Avatar")
                                        .active(true)
                                        .build()))
                        .build())
                .filter(filter)
                .assignmentPanel(assignmentPanel)
                .build();
    }

    private MediaFilterCriteria buildAvatarFilter(
            MediaFilterCriteria filter) {
        MediaFilterCriteria criteria = new MediaFilterCriteria();
        criteria.setOriginalName(filter.getOriginalName());
        criteria.setKind(MediaKind.IMAGE);
        criteria.setProcessingStatus(MediaProcessingStatus.READY);
        criteria.setStatus(RecordStatus.ACTIVE);
        return criteria;
    }

    private UiAssignmentPanelItemView toPanelItem(MediaResult image) {
        return UiAssignmentPanelItemView.builder()
                .title(image.getOriginalName())
                .description(String.format(
                        Locale.ROOT,
                        "%s | %s",
                        formatFileSize(image.getFileSize()),
                        image.getCreatedAt()))
                .imageUrl(image.getThumbnailUrl())
                .action(UiAssignmentActionView.builder()
                        .path(getAvatarPath())
                        .label("Use Avatar")
                        .buttonClass("btn-outline-primary")
                        .targetId(image.getId().toString())
                        .build())
                .build();
    }

    private String getAvatarPath() {
        return getProfilePath() + "/avatar";
    }

    private String getProfilePath() {
        return appProperties.getUi().getHomePath() + "/profile";
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
        return String.format(
                Locale.ROOT,
                "%.1f %s",
                value,
                units[unitIndex]);
    }
}
