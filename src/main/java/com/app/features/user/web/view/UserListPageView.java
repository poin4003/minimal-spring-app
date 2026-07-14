package com.app.features.user.web.view;

import com.app.features.ui.web.component.view.UiDetailModalView;
import com.app.features.ui.web.component.view.UiMetadataModalView;
import com.app.features.ui.web.component.view.UiModalView;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserListPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String heading;
    private final String description;
    private final UiShellView shell;
    private final UiTableView userTable;
    private final UiModalView createUserModal;
    private final UiMetadataModalView metadataModal;
    private final UiDetailModalView detailModal;
    private final String errorMessage;
    private final boolean openCreateUserModal;
    private final boolean openMetadataModal;
    private final boolean openDetailModal;
}
