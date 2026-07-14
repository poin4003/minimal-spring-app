package com.app.features.rbac.web.view;

import com.app.features.ui.web.component.view.UiModalView;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleListPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String heading;
    private final String description;
    private final UiShellView shell;
    private final UiTableView roleTable;
    private final UiModalView createRoleModal;
    private final RoleDetailModalView roleDetailModal;
    private final String successMessage;
    private final String errorMessage;
    private final boolean openCreateRoleModal;
    private final boolean openRoleDetailModal;
}
