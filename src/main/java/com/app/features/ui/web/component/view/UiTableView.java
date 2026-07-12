package com.app.features.ui.web.component.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiTableView {

    private final String title;
    private final String description;
    private final String emptyMessage;
    private final boolean showActions;
    private final List<UiTableColumnView> columns;
    private final List<UiTableRowView> rows;
    private final UiPaginationView pagination;
}
