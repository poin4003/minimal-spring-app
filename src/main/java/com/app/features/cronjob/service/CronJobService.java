package com.app.features.cronjob.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.cronjob.enums.CronjobStatusEnum;
import com.app.features.cronjob.schema.result.CronJobDetailResult;
import com.app.features.cronjob.schema.result.CronJobResult;

public interface CronJobService {

    Page<CronJobResult> getManyCronJobs(Pageable pageable);

    CronJobDetailResult getCronJobDetail(String jobType);

    void refreshRecurringJobs();

    void refreshRecurringJob(String jobType);

    void updateConfig(String jobType, String cronExpression, CronjobStatusEnum status);
}
