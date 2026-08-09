package com.app.features.post.standard.web.support;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.web.view.OwnerPostCardView;
import com.app.features.post.web.enums.OwnerPostActionType;
import com.app.features.post.web.support.OwnerPostLifecycleViewSupport;
import com.app.features.post.web.view.OwnerPostStatusFilterView;
import com.app.features.ui.web.component.view.UiConfirmModalView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OwnerStandardPostViewFactory {

    private final AppProperties appProperties;
    private final OwnerPostLifecycleViewSupport lifecycleViewSupport;

    public OwnerPostCardView toCard(
            OwnerStandardPostResult post,
            boolean detailMode) {
        UUID postId = post.getPost().getId();
        String ownerPath = getOwnerPath();
        return OwnerPostCardView.builder()
                .post(post)
                .detailPath(buildDetailPath(postId))
                .editPath(buildEditPath(postId))
                .statusLabel(lifecycleViewSupport.resolveStatusLabel(
                        post.getState()))
                .statusBadgeClass(
                        lifecycleViewSupport.resolveStatusBadgeClass(
                                post.getState()))
                .actions(lifecycleViewSupport.buildActions(
                        ownerPath,
                        postId,
                        post.getState(),
                        detailMode))
                .editable(lifecycleViewSupport.isEditable(post.getState()))
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
            OwnerStandardPostResult post,
            OwnerPostActionType action) {
        return lifecycleViewSupport.supportsAction(post.getState(), action);
    }

    public UiConfirmModalView buildActionModal(
            OwnerStandardPostResult post,
            OwnerPostActionType action,
            boolean detailMode) {
        return lifecycleViewSupport.buildActionModal(
                getOwnerPath(),
                post.getPost().getId(),
                post.getState(),
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
        return appProperties.getUi().getMyPostsPath();
    }
}
