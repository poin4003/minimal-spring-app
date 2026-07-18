package com.app.features.cronjob.schema.result;

import java.util.UUID;

import com.app.core.enums.RecordStatus;

import lombok.Data;

@Data
public class CronJobResult {

    private UUID id;

    private String jobType;

    private String cronExpression;

    private RecordStatus status;

    private String createdAt;

    private String updatedAt;
}
