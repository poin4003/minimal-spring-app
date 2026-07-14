package com.app.features.rbac.web.view;

import java.util.UUID;

import com.app.features.ui.web.annotation.UiColumn;
import com.app.features.ui.web.enums.UiCellType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionTableRowView {

    private final UUID id;

    @UiColumn(label = "Key", order = 10, type = UiCellType.MONOSPACE)
    private final String key;

    @UiColumn(label = "Name", order = 20)
    private final String name;
}
