package com.app.features.ui.web.component.view;

import com.app.features.ui.web.enums.UiCellType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiTableCellView {

    private final String key;
    private final UiCellType type;
    private final String text;
    private final String badgeClass;
}
