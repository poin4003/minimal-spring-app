package com.app.features.rbac.web.view;

import com.app.features.ui.web.annotation.UiColumn;
import com.app.features.ui.web.enums.UiCellType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoleTableRowView {

    private String id;

    @UiColumn(label = "Key", order = 10, type = UiCellType.MONOSPACE)
    private String key;

    @UiColumn(label = "Name", order = 20)
    private String name;
}
