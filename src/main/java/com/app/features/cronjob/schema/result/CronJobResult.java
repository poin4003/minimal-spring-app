package com.app.features.cronjob.schema.result;

import java.util.UUID;

import com.app.features.cronjob.enums.CronjobStatusEnum;

import lombok.Data;

@Data
public class CronJobResult {

    private UUID id;

    private String jobType;

    private String cronExpression;

    private CronjobStatusEnum status;

    private String createdAt;

    private String updatedAt;
}
