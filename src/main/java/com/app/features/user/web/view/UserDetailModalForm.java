package com.app.features.user.web.view;

import com.app.features.ui.web.annotation.UiField;
import com.app.features.ui.web.enums.UiInputType;
import com.app.features.user.enums.UserStatusEnum;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDetailModalForm {

    @UiField(
            label = "User Id",
            order = 10,
            type = UiInputType.TEXT,
            readOnly = true)
    private String id;

    @UiField(
            label = "Email",
            order = 20,
            type = UiInputType.EMAIL,
            readOnly = true)
    private String email;

    @UiField(
            label = "Login Time",
            order = 30,
            type = UiInputType.TEXT,
            readOnly = true)
    private String loginTime;

    @UiField(
            label = "Logout Time",
            order = 40,
            type = UiInputType.TEXT,
            readOnly = true)
    private String logoutTime;

    @UiField(
            label = "Login IP",
            order = 50,
            type = UiInputType.TEXT,
            readOnly = true)
    private String loginIp;

    @UiField(
            label = "Created At",
            order = 60,
            type = UiInputType.TEXT,
            readOnly = true)
    private String createdAt;

    @UiField(
            label = "Updated At",
            order = 70,
            type = UiInputType.TEXT,
            readOnly = true)
    private String updatedAt;

    @UiField(
            label = "Status",
            order = 80,
            type = UiInputType.SELECT,
            placeholder = "Select status",
            required = true)
    @NotNull(message = "User status is required")
    private UserStatusEnum status;
}
