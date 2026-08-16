package com.app.features.post.videopost.web.support;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.videopost.schema.result.OwnerVideoPostResult;
import com.app.features.post.videopost.web.view.OwnerVideoCardView;
import com.app.features.post.web.enums.OwnerPostActionType;
import com.app.features.post.web.support.OwnerPostLifecycleViewSupport;
import com.app.features.post.web.view.OwnerPostStatusFilterView;
import com.app.features.ui.web.component.view.UiConfirmModalView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OwnerVideoPostViewFactory {

    private final AppProperties appProperties;
    private final OwnerPostLifecycleViewSupport lifecycleViewSupport;

    public OwnerVideoCardView toCard(
            OwnerVideoPostResult video,
            boolean detailMode) {
        UUID postId = video.getPost().getId();
        return OwnerVideoCardView.builder()
                .video(video)
                .detailPath(buildDetailPath(postId))
                .editPath(buildEditPath(postId))
                .statusLabel(lifecycleViewSupport.resolveStatusLabel(
                        video.getState()))
                .statusBadgeClass(
                        lifecycleViewSupport.resolveStatusBadgeClass(
                                video.getState()))
                .actions(lifecycleViewSupport.buildActions(
                        getOwnerPath(),
                        postId,
                        video.getState(),
                        detailMode))
                .editable(lifecycleViewSupport.isEditable(video.getState()))
                .build();
    }

    public List<OwnerPostStatusFilterView> buildStatusFilters(
            PostLifecycleStatus lifecycleStatus,
            PostModerationStatus moderationStatus) {
        return lifecycleViewSupport.buildStatusFilters(
                getOwnerPath(),
                lifecycleStatus,
                moderationStatus);
    }

    public boolean supportsAction(
            OwnerVideoPostResult video,
            OwnerPostActionType action) {
        return lifecycleViewSupport.supportsAction(
                video.getState(),
                action);
    }

    public UiConfirmModalView buildActionModal(
            OwnerVideoPostResult video,
            OwnerPostActionType action,
            boolean detailMode) {
        return lifecycleViewSupport.buildActionModal(
                getOwnerPath(),
                video.getPost().getId(),
                video.getState(),
                action,
                detailMode);
    }

    private String buildDetailPath(UUID postId) {
        return UriComponentsBuilder.fromPath(getOwnerPath())
                .pathSegment(postId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String buildEditPath(UUID postId) {
        return UriComponentsBuilder.fromPath(getOwnerPath())
                .pathSegment(postId.toString(), "edit")
                .build()
                .encode()
                .toUriString();
    }

    private String getOwnerPath() {
        return appProperties.getUi().getMyVideosPath();
    }
}
