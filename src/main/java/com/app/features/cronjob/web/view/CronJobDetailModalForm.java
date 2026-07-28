package com.app.features.cronjob.web.view;

import com.app.core.enums.RecordStatus;
import com.app.features.cronjob.validation.ValidJobRunrCron;
import com.app.features.ui.web.annotation.UiField;
import com.app.features.ui.web.enums.UiInputType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CronJobDetailModalForm {

    @UiField(
            label = "field.jobType",
            order = 10,
            type = UiInputType.TEXT,
            readOnly = true)
    private String jobType;

    @UiField(
            label = "field.jobName",
            order = 20,
            type = UiInputType.TEXT,
            readOnly = true)
    private String name;

    @UiField(
            label = "field.defaultCron",
            order = 30,
            type = UiInputType.TEXT,
            readOnly = true)
    private String defaultCron;

    @UiField(
            label = "field.effectiveCron",
            order = 40,
            type = UiInputType.TEXT,
            readOnly = true)
    private String effectiveCron;

    @UiField(
            label = "field.zoneId",
            order = 50,
            type = UiInputType.TEXT,
            readOnly = true)
    private String zoneId;

    @UiField(
            label = "field.createdAt",
            order = 60,
            type = UiInputType.TEXT,
            readOnly = true)
    private String createdAt;

    @UiField(
            label = "field.updatedAt",
            order = 70,
            type = UiInputType.TEXT,
            readOnly = true)
    private String updatedAt;

    @UiField(
            label = "field.cronExpression",
            order = 80,
            type = UiInputType.TEXT,
            placeholder = "cronjob.form.expressionPlaceholder",
            helpText = "cronjob.form.expressionHelp")
    @Size(max = 100, message = "{validation.cronjob.expression.max}")
    @ValidJobRunrCron(allowBlank = true)
    private String cronExpression;

    @UiField(
            label = "field.status",
            order = 90,
            type = UiInputType.SELECT,
            placeholder = "field.status.select",
            required = true)
    @NotNull(message = "{validation.cronjob.status.required}")
    private RecordStatus status;
}
