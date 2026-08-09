package com.app.features.post.moderation.web.view;

import com.app.features.ui.web.component.view.UiTableView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModerationPostQueueView {

    public static final String ATTRIBUTE = "queue";

    private final String id;
    private final String refreshPath;
    private final String refreshEvent;
    private final UiTableView table;
}
