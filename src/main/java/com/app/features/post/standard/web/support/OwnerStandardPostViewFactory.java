package com.app.features.post.standard.web.support;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.web.enums.OwnerPostActionType;
import com.app.features.post.standard.web.view.OwnerPostActionView;
import com.app.features.post.standard.web.view.OwnerPostCardView;
import com.app.features.post.standard.web.view.OwnerPostStatusFilterView;
import com.app.features.ui.web.component.view.UiConfirmModalView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OwnerStandardPostViewFactory {

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;

    public OwnerPostCardView toCard(
            OwnerStandardPostResult post,
            boolean detailMode) {
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
                .actions(buildActions(post, detailMode))
                .editable(isEditable(post))
                .build();
    }

    public List<OwnerPostStatusFilterView> buildStatusFilters(
            PostLifecycleStatus currentLifecycleStatus,
            PostModerationStatus currentModerationStatus) {
        return List.of(
                buildStatusFilter(
                        null,
                        null,
                        currentLifecycleStatus,
                        currentModerationStatus,
                        "post.owner.filter.all"),
                buildStatusFilter(
                        PostLifecycleStatus.DRAFT,
                        null,
                        currentLifecycleStatus,
                        currentModerationStatus,
                        "post.owner.filter.draft"),
                buildStatusFilter(
                        null,
                        PostModerationStatus.PENDING_REVIEW,
                        currentLifecycleStatus,
                        currentModerationStatus,
                        "post.owner.filter.reviewing"),
                buildStatusFilter(
                        null,
                        PostModerationStatus.PUBLISHED,
                        currentLifecycleStatus,
                        currentModerationStatus,
                        "post.owner.filter.published"),
                buildStatusFilter(
                        null,
                        PostModerationStatus.REJECTED,
                        currentLifecycleStatus,
                        currentModerationStatus,
                        "post.owner.filter.rejected"),
                buildStatusFilter(
                        PostLifecycleStatus.ARCHIVED,
                        null,
                        currentLifecycleStatus,
                        currentModerationStatus,
                        "post.owner.filter.archived"),
                buildStatusFilter(
                        PostLifecycleStatus.DELETED,
                        null,
                        currentLifecycleStatus,
                        currentModerationStatus,
                        "post.owner.filter.trash"));
    }

    public boolean supportsAction(
            OwnerStandardPostResult post,
            OwnerPostActionType action) {
        return buildActionTypes(post).contains(action);
    }

    public UiConfirmModalView buildActionModal(
            OwnerStandardPostResult post,
            OwnerPostActionType action,
            boolean detailMode) {
        return UiConfirmModalView.builder()
                .id("owner-post-" + action.getPath() + "-modal")
                .title(messageResolver.get(resolveActionTitleKey(action)))
                .description(messageResolver.get(
                        resolveActionDescriptionKey(action)))
                .actionPath(buildActionPath(
                        post.getId(),
                        action,
                        detailMode))
                .confirmLabel(messageResolver.get(
                        resolveActionLabelKey(action)))
                .confirmButtonClass(resolveConfirmButtonClass(action))
                .build();
    }

    private OwnerPostStatusFilterView buildStatusFilter(
            PostLifecycleStatus lifecycleStatus,
            PostModerationStatus moderationStatus,
            PostLifecycleStatus currentLifecycleStatus,
            PostModerationStatus currentModerationStatus,
            String labelKey) {
        return OwnerPostStatusFilterView.builder()
                .label(messageResolver.get(labelKey))
                .path(buildFilterPath(lifecycleStatus, moderationStatus))
                .active(lifecycleStatus == currentLifecycleStatus
                        && moderationStatus == currentModerationStatus)
                .build();
    }

    private String buildFilterPath(
            PostLifecycleStatus lifecycleStatus,
            PostModerationStatus moderationStatus) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(
                appProperties.getUi().getMyPostsPath());
        if (lifecycleStatus != null) {
            builder.queryParam(
                    "lifecycleStatus",
                    lifecycleStatus.name());
        }
        if (moderationStatus != null) {
            builder.queryParam(
                    "moderationStatus",
                    moderationStatus.name());
        }
        return builder.build().encode().toUriString();
    }

    private List<OwnerPostActionView> buildActions(
            OwnerStandardPostResult post,
            boolean detailMode) {
        return buildActionTypes(post).stream()
                .map(action -> OwnerPostActionView.builder()
                        .label(messageResolver.get(
                                resolveActionLabelKey(action)))
                        .modalPath(buildActionConfirmPath(
                                post.getId(),
                                action,
                                detailMode))
                        .iconClass(resolveActionIconClass(action))
                        .buttonClass(resolveActionButtonClass(action))
                        .build())
                .toList();
    }

    private List<OwnerPostActionType> buildActionTypes(
            OwnerStandardPostResult post) {
        List<OwnerPostActionType> actions = new ArrayList<>();
        switch (post.getLifecycleStatus()) {
            case DRAFT -> {
                actions.add(OwnerPostActionType.SUBMIT);
                actions.add(OwnerPostActionType.DELETE);
            }
            case ACTIVE -> {
                if (post.getModerationStatus()
                        == PostModerationStatus.PUBLISHED) {
                    actions.add(OwnerPostActionType.ARCHIVE);
                }
                actions.add(OwnerPostActionType.DELETE);
            }
            case ARCHIVED -> {
                actions.add(OwnerPostActionType.RESTORE_ARCHIVED);
                actions.add(OwnerPostActionType.DELETE);
            }
            case DELETED -> actions.add(
                    OwnerPostActionType.RESTORE_DELETED);
        }
        return List.copyOf(actions);
    }

    private boolean isEditable(OwnerStandardPostResult post) {
        return post.getLifecycleStatus() != PostLifecycleStatus.ARCHIVED
                && post.getLifecycleStatus() != PostLifecycleStatus.DELETED;
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

    private String buildActionConfirmPath(
            UUID postId,
            OwnerPostActionType action,
            boolean detailMode) {
        return buildActionPath(postId, action, detailMode, "confirm");
    }

    private String buildActionPath(
            UUID postId,
            OwnerPostActionType action,
            boolean detailMode,
            String... segments) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(
                        appProperties.getUi().getMyPostsPath())
                .pathSegment(
                        postId.toString(),
                        "actions",
                        action.getPath());
        for (String segment : segments) {
            builder.pathSegment(segment);
        }
        if (detailMode) {
            builder.queryParam("detail", true);
        }
        return builder.build().encode().toUriString();
    }

    private String buildActionPath(
            UUID postId,
            OwnerPostActionType action,
            boolean detailMode) {
        return buildActionPath(postId, action, detailMode, new String[0]);
    }

    private String resolveActionLabelKey(OwnerPostActionType action) {
        return switch (action) {
            case SUBMIT -> "post.owner.action.submit";
            case ARCHIVE -> "post.owner.action.archive";
            case RESTORE_ARCHIVED, RESTORE_DELETED ->
                "post.owner.action.restore";
            case DELETE -> "post.owner.action.delete";
        };
    }

    private String resolveActionTitleKey(OwnerPostActionType action) {
        return switch (action) {
            case SUBMIT -> "post.owner.action.submit.title";
            case ARCHIVE -> "post.owner.action.archive.title";
            case RESTORE_ARCHIVED ->
                "post.owner.action.restoreArchived.title";
            case DELETE -> "post.owner.action.delete.title";
            case RESTORE_DELETED ->
                "post.owner.action.restoreDeleted.title";
        };
    }

    private String resolveActionDescriptionKey(
            OwnerPostActionType action) {
        return switch (action) {
            case SUBMIT -> "post.owner.action.submit.description";
            case ARCHIVE -> "post.owner.action.archive.description";
            case RESTORE_ARCHIVED ->
                "post.owner.action.restoreArchived.description";
            case DELETE -> "post.owner.action.delete.description";
            case RESTORE_DELETED ->
                "post.owner.action.restoreDeleted.description";
        };
    }

    private String resolveActionIconClass(OwnerPostActionType action) {
        return switch (action) {
            case SUBMIT -> "bi bi-send";
            case ARCHIVE -> "bi bi-archive";
            case RESTORE_ARCHIVED, RESTORE_DELETED ->
                "bi bi-arrow-counterclockwise";
            case DELETE -> "bi bi-trash";
        };
    }

    private String resolveActionButtonClass(OwnerPostActionType action) {
        return switch (action) {
            case SUBMIT -> "btn-outline-primary";
            case ARCHIVE -> "btn-outline-secondary";
            case RESTORE_ARCHIVED, RESTORE_DELETED ->
                "btn-outline-success";
            case DELETE -> "btn-outline-danger";
        };
    }

    private String resolveConfirmButtonClass(
            OwnerPostActionType action) {
        return switch (action) {
            case SUBMIT -> "btn-primary";
            case ARCHIVE -> "btn-secondary";
            case RESTORE_ARCHIVED, RESTORE_DELETED -> "btn-success";
            case DELETE -> "btn-danger";
        };
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
