package com.app.features.post.moderation.web.view;

import java.util.List;

import com.app.core.schema.query.UiPageQuery;
import com.app.features.post.moderation.schema.filter.ModerationPostFilterCriteria;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModerationPostListPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String listPath;
    private final UiShellView shell;
    private final ModerationPostFilterCriteria filter;
    private final UiPageQuery query;
    private final List<ModerationPostStatusOptionView> moderationStatuses;
    private final List<ModerationPostTypeOptionView> postTypes;
    private final ModerationPostQueueView queue;
}
