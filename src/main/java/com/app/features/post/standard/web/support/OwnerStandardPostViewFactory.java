package com.app.features.post.standard.web.support;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.web.view.OwnerPostCardView;
import com.app.features.post.standard.web.view.OwnerPostStatusFilterView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OwnerStandardPostViewFactory {

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;

    public OwnerPostCardView toCard(OwnerStandardPostResult post) {
        return OwnerPostCardView.builder()
                .post(post)
                .detailPath(buildDetailPath(post.getId()))
                .editPath(buildEditPath(post.getId()))
                .statusLabel(resolveStatusLabel(
                        post.getLifecycleStatus(),
                        post.getModerationStatus()))
                .statusBadgeClass(resolveStatusBadgeClass(
                        post.getLifecycleStatus(),
                        post.getModerationStatus()))
                .build();
    }

    public List<OwnerPostStatusFilterView> buildStatusFilters(
            PostModerationStatus currentStatus) {
        return List.of(
                buildStatusFilter(null, currentStatus),
                buildStatusFilter(
                        PostModerationStatus.PENDING_REVIEW,
                        currentStatus),
                buildStatusFilter(
                        PostModerationStatus.PUBLISHED,
                        currentStatus),
                buildStatusFilter(
                        PostModerationStatus.REJECTED,
                        currentStatus));
    }

    private OwnerPostStatusFilterView buildStatusFilter(
            PostModerationStatus status,
            PostModerationStatus currentStatus) {
        return OwnerPostStatusFilterView.builder()
                .label(status == null
                        ? messageResolver.get("post.owner.filter.all")
                        : resolveModerationStatusLabel(status))
                .path(buildFilterPath(status))
                .active(status == currentStatus)
                .build();
    }

    private String buildFilterPath(PostModerationStatus status) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(
                appProperties.getUi().getMyPostsPath());
        if (status != null) {
            builder.queryParam("moderationStatus", status.name());
        }
        return builder.build().encode().toUriString();
    }

    private String buildDetailPath(UUID postId) {
        return UriComponentsBuilder.fromPath(
                        appProperties.getUi().getMyPostsPath())
                .pathSegment(postId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String buildEditPath(UUID postId) {
        return UriComponentsBuilder.fromPath(
                        appProperties.getUi().getMyPostsPath())
                .pathSegment(postId.toString(), "edit")
                .build()
                .encode()
                .toUriString();
    }

    private String resolveStatusLabel(
            PostLifecycleStatus lifecycleStatus,
            PostModerationStatus moderationStatus) {
        return switch (lifecycleStatus) {
            case DRAFT -> messageResolver.get(
                    "post.lifecycleStatus.draft");
            case ARCHIVED -> messageResolver.get(
                    "post.lifecycleStatus.archived");
            case DELETED -> messageResolver.get(
                    "post.lifecycleStatus.deleted");
            case ACTIVE -> resolveModerationStatusLabel(moderationStatus);
        };
    }

    private String resolveModerationStatusLabel(PostModerationStatus status) {
        return switch (status) {
            case PENDING_REVIEW -> messageResolver.get(
                    "post.moderationStatus.pendingReview");
            case PUBLISHED -> messageResolver.get(
                    "post.moderationStatus.published");
            case REJECTED -> messageResolver.get(
                    "post.moderationStatus.rejected");
        };
    }

    private String resolveStatusBadgeClass(
            PostLifecycleStatus lifecycleStatus,
            PostModerationStatus moderationStatus) {
        return switch (lifecycleStatus) {
            case DRAFT -> "text-bg-secondary";
            case ARCHIVED -> "text-bg-dark";
            case DELETED -> "text-bg-danger";
            case ACTIVE -> switch (moderationStatus) {
                case PENDING_REVIEW -> "text-bg-warning";
                case PUBLISHED -> "text-bg-success";
                case REJECTED -> "text-bg-danger";
            };
        };
    }
}
