package com.app.features.rbac.web.view;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.app.features.ui.web.annotation.UiField;
import com.app.features.ui.web.enums.UiInputType;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoleDetailModalForm {

    @UiField(
            label = "field.roleName",
            order = 10,
            type = UiInputType.TEXT,
            placeholder = "rbac.role.form.namePlaceholder",
            required = true)
    @NotBlank(message = "{validation.rbac.roleName.required}")
    private String name;

    @UiField(
            label = "field.roleKey",
            order = 20,
            type = UiInputType.TEXT,
            placeholder = "rbac.role.form.keyPlaceholder",
            helpText = "rbac.role.form.keyHelp",
            required = true)
    @NotBlank(message = "{validation.rbac.roleKey.required}")
    private String key;

    @UiField(
            label = "field.createdAt",
            order = 30,
            type = UiInputType.TEXT,
            readOnly = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAt;

    @UiField(
            label = "field.updatedAt",
            order = 40,
            type = UiInputType.TEXT,
            readOnly = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedAt;
}
