package com.app.features.rbac.web.view;

import java.util.UUID;

import com.app.features.ui.web.annotation.UiField;
import com.app.features.ui.web.enums.UiInputType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RolePermissionActionForm {

    @UiField(
            label = "Permission",
            order = 10,
            type = UiInputType.SELECT,
            placeholder = "Choose a permission",
            required = true)
    @NotNull(message = "Permission is required")
    private UUID permissionId;
}
