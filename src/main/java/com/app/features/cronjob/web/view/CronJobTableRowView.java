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

    @UiColumn(label = "Job Type", order = 10, type = UiCellType.MONOSPACE)
    private String jobType;

    @UiColumn(label = "Cron Override", order = 20, type = UiCellType.MONOSPACE, emptyValue = "Default cron")
    private String cronExpression;

    @UiColumn(label = "Status", order = 30, type = UiCellType.BADGE)
    private RecordStatus status;

    @UiColumn(label = "Updated At", order = 40)
    private String updatedAt;
}
