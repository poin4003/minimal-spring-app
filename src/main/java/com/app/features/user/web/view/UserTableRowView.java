package com.app.features.user.web.view;

import java.util.UUID;

import com.app.features.ui.web.annotation.UiColumn;
import com.app.features.ui.web.enums.UiCellType;
import com.app.features.user.enums.UserStatusEnum;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserTableRowView {

    private final UUID id;

    @UiColumn(label = "Email", order = 10)
    private final String email;

    @UiColumn(label = "Status", order = 20, type = UiCellType.BADGE)
    private final UserStatusEnum status;

    @UiColumn(label = "Created At", order = 30)
    private final String createdAt;

    @UiColumn(label = "Updated At", order = 40)
    private final String updatedAt;
}
