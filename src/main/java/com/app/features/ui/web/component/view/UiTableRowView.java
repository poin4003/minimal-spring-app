package com.app.features.ui.web.component.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiTableRowView {

    private final List<UiTableCellView> cells;
    private final List<UiTableActionView> actions;
}
