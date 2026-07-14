package com.app.features.ui.web.component.view;

import com.app.features.ui.web.query.UiAssignmentPageQuery;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiAssignmentActionView {

    private final String path;
    private final String label;
    private final String buttonClass;
    private final String targetId;
    private final UiAssignmentPageQuery query;
}
