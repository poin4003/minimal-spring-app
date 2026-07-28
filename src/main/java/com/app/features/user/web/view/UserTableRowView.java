package com.app.features.user.web.view;

import java.util.UUID;

import com.app.features.ui.web.annotation.UiColumn;
import com.app.features.ui.web.enums.UiCellType;
import com.app.features.user.enums.UserStatusEnum;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserTableRowView {

    private UUID id;

    @UiColumn(label = "field.email", order = 10)
    private String email;

    @UiColumn(label = "field.status", order = 20, type = UiCellType.BADGE)
    private UserStatusEnum status;

    @UiColumn(label = "field.createdAt", order = 30)
    private String createdAt;

    @UiColumn(label = "field.updatedAt", order = 40)
    private String updatedAt;
}
