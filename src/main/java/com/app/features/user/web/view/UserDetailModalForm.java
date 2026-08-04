package com.app.features.user.web.view;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

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
            label = "field.userId",
            order = 10,
            type = UiInputType.TEXT,
            readOnly = true)
    private String id;

    @UiField(
            label = "field.email",
            order = 20,
            type = UiInputType.EMAIL,
            readOnly = true)
    private String email;

    @UiField(
            label = "field.loginTime",
            order = 30,
            type = UiInputType.TEXT,
            readOnly = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime loginTime;

    @UiField(
            label = "field.logoutTime",
            order = 40,
            type = UiInputType.TEXT,
            readOnly = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime logoutTime;

    @UiField(
            label = "field.loginIp",
            order = 50,
            type = UiInputType.TEXT,
            readOnly = true)
    private String loginIp;

    @UiField(
            label = "field.createdAt",
            order = 60,
            type = UiInputType.TEXT,
            readOnly = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAt;

    @UiField(
            label = "field.updatedAt",
            order = 70,
            type = UiInputType.TEXT,
            readOnly = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedAt;

    @UiField(
            label = "field.status",
            order = 80,
            type = UiInputType.SELECT,
            placeholder = "field.status.select",
            required = true)
    @NotNull(message = "{validation.user.status.required}")
    private UserStatusEnum status;
}
