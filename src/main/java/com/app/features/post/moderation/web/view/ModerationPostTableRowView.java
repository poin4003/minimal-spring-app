package com.app.features.post.moderation.web.view;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.post.enums.PostType;
import com.app.features.ui.web.annotation.UiColumn;
import com.app.features.ui.web.enums.UiCellType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModerationPostTableRowView {

    private final UUID id;
    private final PostType type;

    @UiColumn(
            label = "post.moderation.table.type",
            order = 1,
            type = UiCellType.BADGE,
            badgeClass = "text-bg-info")
    private final String typeLabel;

    @UiColumn(
            label = "post.moderation.table.author",
            order = 2)
    private final String authorName;

    @UiColumn(
            label = "post.moderation.table.status",
            order = 3,
            type = UiCellType.BADGE,
            badgeClass = "text-bg-warning")
    private final String moderationStatusLabel;

    @UiColumn(
            label = "post.moderation.table.submittedAt",
            order = 4)
    private final LocalDateTime createdAt;
}
