package com.app.features.rbac.web.view;

import com.app.features.ui.web.component.view.UiModalView;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RolePermissionPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String heading;
    private final String description;
    private final String roleKey;
    private final String roleName;
    private final UiShellView shell;
    private final UiTableView assignedPermissionTable;
    private final UiModalView assignPermissionModal;
    private final UiModalView removePermissionModal;
    private final String successMessage;
    private final String errorMessage;
    private final boolean openAssignPermissionModal;
    private final boolean openRemovePermissionModal;
}
