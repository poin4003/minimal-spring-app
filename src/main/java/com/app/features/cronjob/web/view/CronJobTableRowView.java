package com.app.features.cronjob.web.view;

import com.app.core.enums.RecordStatus;
import com.app.features.ui.web.annotation.UiColumn;
import com.app.features.ui.web.enums.UiCellType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CronJobTableRowView {

    @UiColumn(label = "field.jobType", order = 10, type = UiCellType.MONOSPACE)
    private String jobType;

    @UiColumn(
            label = "field.cronOverride",
            order = 20,
            type = UiCellType.MONOSPACE,
            emptyValue = "cronjob.defaultCron")
    private String cronExpression;

    @UiColumn(label = "field.status", order = 30, type = UiCellType.BADGE)
    private RecordStatus status;

    @UiColumn(label = "field.updatedAt", order = 40)
    private String updatedAt;
}
