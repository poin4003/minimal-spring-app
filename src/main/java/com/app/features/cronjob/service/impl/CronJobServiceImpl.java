package com.app.features.cronjob.service.impl;

import org.jobrunr.storage.StorageProvider;
import org.modelmapper.ModelMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.enums.RecordStatus;
import com.app.core.exception.ExceptionFactory;
import com.app.features.cronjob.entity.CronJobConfigEntity;
import com.app.features.cronjob.repository.CronJobConfigRepository;
import com.app.features.cronjob.scheduler.RecurringJobDefinition;
import com.app.features.cronjob.scheduler.RecurringJobRegistry;
import com.app.features.cronjob.schema.result.CronJobDetailResult;
import com.app.features.cronjob.schema.result.CronJobResult;
import com.app.features.cronjob.service.CronJobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CronJobServiceImpl implements CronJobService {

    private final StorageProvider storageProvider;
    private final CronJobConfigRepository jobConfigRepo;
    private final RecurringJobRegistry recurringJobRegistry;
    private final ModelMapper mapper;

    @Override
    public Page<CronJobResult> getManyCronJobs(Pageable pageable) {
        Page<CronJobConfigEntity> entityPage = jobConfigRepo.findAll(pageable);

        return entityPage.map(result -> mapper.map(result, CronJobResult.class));
    }

    @Override
    public CronJobDetailResult getCronJobDetail(String jobType) {
        CronJobConfigEntity config = jobConfigRepo.findByJobType(jobType)
                .orElseThrow(() -> ExceptionFactory.notFound("Cronjob config: " + jobType));

        return toCronJobDetailResult(config);
    }

    @Override
    public void refreshRecurringJobs() {
        for (CronJobConfigEntity config : jobConfigRepo.findAll()) {
            refreshRecurringJob(config.getJobType());
        }
    }

    @Override
    public void refreshRecurringJob(String jobType) {
        RecurringJobDefinition definition = recurringJobRegistry.getRequired(jobType);

        CronJobConfigEntity config = jobConfigRepo.findByJobType(jobType)
                .orElseThrow(() -> ExceptionFactory.notFound("Cronjob config: " + jobType));

        if (config.getStatus() == RecordStatus.INACTIVE) {
            storageProvider.deleteRecurringJob(jobType);
            log.info("Disabled recurring job [{}]", jobType);
            return;
        }

        String cron = definition.resolveCron(config.getCronExpression());

        storageProvider.saveRecurringJob(definition.toRecurringJob(cron));
        log.info("Registered recurring job [{}] with cron [{}]", jobType, cron);
    }

    @Transactional
    @Override
    public void updateConfig(String jobType, String cronExpression, RecordStatus status) {
        CronJobConfigEntity config = jobConfigRepo.findByJobType(jobType)
                .orElseThrow(() -> ExceptionFactory.notFound("Cronjob config: " + jobType));

        config.setCronExpression(normalizeCronExpression(cronExpression));
        config.setStatus(status);
        jobConfigRepo.save(config);

        refreshRecurringJob(jobType);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        refreshRecurringJobs();
    }

    private CronJobDetailResult toCronJobDetailResult(CronJobConfigEntity config) {
        RecurringJobDefinition definition = recurringJobRegistry.getRequired(config.getJobType());

        CronJobDetailResult result = mapper.map(config, CronJobDetailResult.class);
        result.setName(definition.getName());
        result.setDefaultCron(definition.getDefaultCron());
        result.setEffectiveCron(definition.resolveCron(config.getCronExpression()));
        result.setZoneId(definition.getZoneId());
        result.setUsingDefaultCron(config.getCronExpression() == null || config.getCronExpression().isBlank());

        return result;
    }

    private String normalizeCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return null;
        }
        return cronExpression.trim();
    }
}
