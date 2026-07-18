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
            label = "Job Type",
            order = 10,
            type = UiInputType.TEXT,
            readOnly = true)
    private String jobType;

    @UiField(
            label = "Job Name",
            order = 20,
            type = UiInputType.TEXT,
            readOnly = true)
    private String name;

    @UiField(
            label = "Default Cron",
            order = 30,
            type = UiInputType.TEXT,
            readOnly = true)
    private String defaultCron;

    @UiField(
            label = "Effective Cron",
            order = 40,
            type = UiInputType.TEXT,
            readOnly = true)
    private String effectiveCron;

    @UiField(
            label = "Zone Id",
            order = 50,
            type = UiInputType.TEXT,
            readOnly = true)
    private String zoneId;

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
            label = "Cron Expression",
            order = 80,
            type = UiInputType.TEXT,
            placeholder = "0 0 1 * * *",
            helpText = "Leave blank to use the default cron from the job annotation.")
    @Size(max = 100, message = "Cron expression must be less than or equal to 100 characters")
    @ValidJobRunrCron(allowBlank = true)
    private String cronExpression;

    @UiField(
            label = "Status",
            order = 90,
            type = UiInputType.SELECT,
            placeholder = "Select status",
            required = true)
    @NotNull(message = "Cronjob status is required")
    private RecordStatus status;
}
