package com.app.features.post.aimoderation.web.view;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.post.aimoderation.enums.PostAiModerationOutcome;
import com.app.features.ui.web.annotation.UiColumn;
import com.app.features.ui.web.enums.UiCellType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostAiModerationDecisionLogTableRowView {

    private final UUID id;

    @UiColumn(
            label = "post.aiModeration.log.table.postId",
            order = 1,
            type = UiCellType.MONOSPACE)
    private final UUID postId;

    @UiColumn(
            label = "post.aiModeration.log.table.outcome",
            order = 2,
            type = UiCellType.BADGE)
    private final PostAiModerationOutcome outcome;

    @UiColumn(
            label = "post.aiModeration.log.table.decision",
            order = 3)
    private final String decisionSummary;

    @UiColumn(
            label = "post.aiModeration.log.table.model",
            order = 4)
    private final String modelName;

    @UiColumn(
            label = "post.aiModeration.log.table.createdAt",
            order = 5)
    private final LocalDateTime createdAt;
}
