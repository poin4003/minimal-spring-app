package com.app.features.post.web.support;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.core.i18n.AppMessageResolver;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.schema.result.OwnerPostStateResult;
import com.app.features.post.web.enums.OwnerPostActionType;
import com.app.features.post.web.view.OwnerPostActionView;
import com.app.features.post.web.view.OwnerPostStatusFilterView;
import com.app.features.ui.web.component.view.UiConfirmModalView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OwnerPostLifecycleViewSupport {

    private final AppMessageResolver messageResolver;

    public List<OwnerPostStatusFilterView> buildStatusFilters(
            String ownerPath,
            PostLifecycleStatus currentLifecycleStatus,
            PostModerationStatus currentModerationStatus) {
        return List.of(
                buildStatusFilter(ownerPath, null, null,
                        currentLifecycleStatus, currentModerationStatus,
                        "post.owner.filter.all"),
                buildStatusFilter(ownerPath, PostLifecycleStatus.DRAFT, null,
                        currentLifecycleStatus, currentModerationStatus,
                        "post.owner.filter.draft"),
                buildStatusFilter(ownerPath, null,
                        PostModerationStatus.PENDING_REVIEW,
                        currentLifecycleStatus, currentModerationStatus,
                        "post.owner.filter.reviewing"),
                buildStatusFilter(ownerPath, null,
                        PostModerationStatus.PUBLISHED,
                        currentLifecycleStatus, currentModerationStatus,
                        "post.owner.filter.published"),
                buildStatusFilter(ownerPath, null,
                        PostModerationStatus.REJECTED,
                        currentLifecycleStatus, currentModerationStatus,
                        "post.owner.filter.rejected"),
                buildStatusFilter(ownerPath, PostLifecycleStatus.ARCHIVED, null,
                        currentLifecycleStatus, currentModerationStatus,
                        "post.owner.filter.archived"),
                buildStatusFilter(ownerPath, PostLifecycleStatus.DELETED, null,
                        currentLifecycleStatus, currentModerationStatus,
                        "post.owner.filter.trash"));
    }

    public boolean supportsAction(
            OwnerPostStateResult state,
            OwnerPostActionType action) {
        return buildActionTypes(state).contains(action);
    }

    public UiConfirmModalView buildActionModal(
            String ownerPath,
            UUID postId,
            OwnerPostStateResult state,
            OwnerPostActionType action,
            boolean detailMode) {
        if (!supportsAction(state, action)) {
            return null;
        }
        return UiConfirmModalView.builder()
                .id("owner-post-" + action.getPath() + "-modal")
                .title(messageResolver.get(resolveActionTitleKey(action)))
                .description(messageResolver.get(
                        resolveActionDescriptionKey(action)))
                .actionPath(buildActionPath(
                        ownerPath, postId, action, detailMode))
                .confirmLabel(messageResolver.get(
                        resolveActionLabelKey(action)))
                .confirmButtonClass(resolveConfirmButtonClass(action))
                .build();
    }

    public List<OwnerPostActionView> buildActions(
            String ownerPath,
            UUID postId,
            OwnerPostStateResult state,
            boolean detailMode) {
        return buildActionTypes(state).stream()
                .map(action -> OwnerPostActionView.builder()
                        .label(messageResolver.get(
                                resolveActionLabelKey(action)))
                        .modalPath(buildActionConfirmPath(
                                ownerPath, postId, action, detailMode))
                        .iconClass(resolveActionIconClass(action))
                        .buttonClass(resolveActionButtonClass(action))
                        .build())
                .toList();
    }

    public boolean isEditable(OwnerPostStateResult state) {
        return state.getLifecycleStatus() != PostLifecycleStatus.ARCHIVED
                && state.getLifecycleStatus() != PostLifecycleStatus.DELETED;
    }

    public String resolveStatusLabel(OwnerPostStateResult state) {
        return switch (state.getLifecycleStatus()) {
            case DRAFT -> messageResolver.get("post.lifecycleStatus.draft");
            case ARCHIVED -> messageResolver.get("post.lifecycleStatus.archived");
            case DELETED -> messageResolver.get("post.lifecycleStatus.deleted");
            case ACTIVE -> resolveModerationStatusLabel(
                    state.getModerationStatus());
        };
    }

    public String resolveStatusBadgeClass(OwnerPostStateResult state) {
        return switch (state.getLifecycleStatus()) {
            case DRAFT -> "text-bg-secondary";
            case ARCHIVED -> "text-bg-dark";
            case DELETED -> "text-bg-danger";
            case ACTIVE -> switch (state.getModerationStatus()) {
                case PENDING_REVIEW -> "text-bg-warning";
                case PUBLISHED -> "text-bg-success";
                case REJECTED -> "text-bg-danger";
            };
        };
    }

    private OwnerPostStatusFilterView buildStatusFilter(
            String ownerPath,
            PostLifecycleStatus lifecycleStatus,
            PostModerationStatus moderationStatus,
            PostLifecycleStatus currentLifecycleStatus,
            PostModerationStatus currentModerationStatus,
            String labelKey) {
        return OwnerPostStatusFilterView.builder()
                .label(messageResolver.get(labelKey))
                .path(buildFilterPath(
                        ownerPath, lifecycleStatus, moderationStatus))
                .active(lifecycleStatus == currentLifecycleStatus
                        && moderationStatus == currentModerationStatus)
                .build();
    }

    private String buildFilterPath(
            String ownerPath,
            PostLifecycleStatus lifecycleStatus,
            PostModerationStatus moderationStatus) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(ownerPath);
        if (lifecycleStatus != null) {
            builder.queryParam("lifecycleStatus", lifecycleStatus.name());
        }
        if (moderationStatus != null) {
            builder.queryParam("moderationStatus", moderationStatus.name());
        }
        return builder.build().encode().toUriString();
    }

    private List<OwnerPostActionType> buildActionTypes(
            OwnerPostStateResult state) {
        List<OwnerPostActionType> actions = new ArrayList<>();
        switch (state.getLifecycleStatus()) {
            case DRAFT -> {
                actions.add(OwnerPostActionType.SUBMIT);
                actions.add(OwnerPostActionType.DELETE);
            }
            case ACTIVE -> {
                if (state.getModerationStatus()
                        == PostModerationStatus.PUBLISHED) {
                    actions.add(OwnerPostActionType.ARCHIVE);
                }
                actions.add(OwnerPostActionType.DELETE);
            }
            case ARCHIVED -> {
                actions.add(OwnerPostActionType.RESTORE_ARCHIVED);
                actions.add(OwnerPostActionType.DELETE);
            }
            case DELETED -> actions.add(OwnerPostActionType.RESTORE_DELETED);
        }
        return List.copyOf(actions);
    }

    private String buildActionConfirmPath(
            String ownerPath,
            UUID postId,
            OwnerPostActionType action,
            boolean detailMode) {
        return buildActionPath(
                ownerPath, postId, action, detailMode, "confirm");
    }

    private String buildActionPath(
            String ownerPath,
            UUID postId,
            OwnerPostActionType action,
            boolean detailMode,
            String... segments) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(ownerPath)
                .pathSegment(postId.toString(), action.getPath());
        for (String segment : segments) {
            builder.pathSegment(segment);
        }
        if (detailMode) {
            builder.queryParam("detail", true);
        }
        return builder.build().encode().toUriString();
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

    private String resolveActionDescriptionKey(OwnerPostActionType action) {
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
            case RESTORE_ARCHIVED, RESTORE_DELETED -> "btn-outline-success";
            case DELETE -> "btn-outline-danger";
        };
    }

    private String resolveConfirmButtonClass(OwnerPostActionType action) {
        return switch (action) {
            case SUBMIT -> "btn-primary";
            case ARCHIVE -> "btn-secondary";
            case RESTORE_ARCHIVED, RESTORE_DELETED -> "btn-success";
            case DELETE -> "btn-danger";
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
}
