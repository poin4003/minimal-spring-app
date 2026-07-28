package com.app.features.cronjob.schema.payload;

import com.app.core.enums.RecordStatus;
import com.app.features.cronjob.validation.ValidJobRunrCron;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCronJobConfigPayload {

    @Size(max = 100, message = "{validation.cronjob.expression.max}")
    @ValidJobRunrCron(allowBlank = true)
    private String cronExpression;

    @NotNull(message = "{validation.cronjob.status.required}")
    private RecordStatus status;
}
