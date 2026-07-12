package com.app.features.ui.web.component.view;

import com.app.features.ui.web.enums.UiCellType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiTableColumnView {

    private final String key;
    private final String label;
    private final UiCellType type;
}
