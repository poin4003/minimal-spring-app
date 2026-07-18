package com.app.features.cronjob.schema.payload;

import com.app.core.enums.RecordStatus;
import com.app.features.cronjob.validation.ValidJobRunrCron;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCronJobConfigPayload {

    @Size(max = 100, message = "Cron expression must be less than or equal to 100 characters")
    @ValidJobRunrCron(allowBlank = true)
    private String cronExpression;

    @NotNull(message = "Cronjob status is required")
    private RecordStatus status;
}
