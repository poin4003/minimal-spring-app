package com.app.features.rbac.web.view;

import com.app.features.ui.web.annotation.UiField;
import com.app.features.ui.web.enums.UiInputType;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateRoleModalForm {

    @UiField(
            label = "Role Name",
            order = 10,
            type = UiInputType.TEXT,
            placeholder = "Super Admin",
            required = true)
    @NotBlank(message = "Role name is required")
    private String name;

    @UiField(
            label = "Role Key",
            order = 20,
            type = UiInputType.TEXT,
            placeholder = "SUPER_ADMIN",
            helpText = "Use an uppercase key for authorization mapping.",
            required = true)
    @NotBlank(message = "Role key is required")
    private String key;
}
