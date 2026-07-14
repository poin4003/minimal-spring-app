package com.app.features.ui.web.component.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiAssignmentPanelView {

    private final String title;
    private final String description;
    private final String emptyMessage;
    private final List<UiAssignmentPanelItemView> rows;
    private final UiPaginationView pagination;
}
