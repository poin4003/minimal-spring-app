package com.app.features.cronjob.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.app.core.cache.RedisService;
import com.app.features.cronjob.entity.CronJobConfigEntity;
import com.app.features.cronjob.enums.CronjobStatusEnum;
import com.app.features.cronjob.repository.CronJobConfigRepository;
import com.app.features.cronjob.scheduler.JobHandler;
import com.app.features.cronjob.service.CronJobService;

// import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;

@Slf4j
@Service
public class CronJobServiceImpl implements CronJobService {

    private final TaskScheduler taskScheduler;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final CronJobConfigRepository jobConfigRepo;
    private final Map<String, JobHandler> jobHandlerMap;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    private final RedisService redisService;
    private static final String CRON_STATUS_PREFIX = "cronjob:status";

    public CronJobServiceImpl(
            TaskScheduler taskScheduler,
            LockingTaskExecutor lockingTaskExecutor,
            CronJobConfigRepository jobConfigRepo,
            List<JobHandler> jobHandlers,
            RedisService redisService) {
        this.taskScheduler = taskScheduler;
        this.lockingTaskExecutor = lockingTaskExecutor;
        this.jobConfigRepo = jobConfigRepo;
        this.redisService = redisService;

        this.jobHandlerMap = jobHandlers.stream()
                .collect(Collectors.toMap(JobHandler::getSupportedJobType, Function.identity()));
    }

    // @PostConstruct
    @Override
    public void refreshJobs() {
        log.info("Refreshing dynamic jobs from Database...");

        // Get all active jobs
        List<CronJobConfigEntity> configs = jobConfigRepo.findByStatus(CronjobStatusEnum.ACTIVE);

        scheduledTasks.forEach((k, v) -> {
            v.cancel(false);
            log.debug("Cancelled job: {}", k);
        });
        scheduledTasks.clear();

        for (CronJobConfigEntity config : configs) {
            scheduleSingleJob(config);
        }

        log.info("Refreshed {} jobs successfully.", scheduledTasks.size());
    }

    private void scheduleSingleJob(CronJobConfigEntity config) {
        try {
            String jobName = config.getName();

            if (!isJobEnabled(jobName)) {
                log.warn("Job [{}] is DISABLED in Redis. Skipping.", jobName);
                return;
            }

            JobHandler handler = jobHandlerMap.get(config.getJobType());
            if (handler == null) {
                log.error("No Handler found for job type: [{}]. Skipping.", config.getJobType());
                return;
            }

            Runnable lockableTask = () -> {
                Duration lockAtMost = Duration.parse(config.getLockAtMostFor());
                Duration lockAtLeast = Duration.parse(config.getLockAtLeastFor());

                LockConfiguration lockConfig = new LockConfiguration(
                        Instant.now(),
                        jobName,
                        lockAtMost,
                        lockAtLeast);
                lockingTaskExecutor.executeWithLock((Runnable) () -> {
                    log.info("Starting job: {}", jobName);
                    handler.execute();
                    log.info("Finished job: {}", jobName);
                }, lockConfig);
            };

            ScheduledFuture<?> future = taskScheduler.schedule(
                    lockableTask,
                    new CronTrigger(
                            Objects.requireNonNull(config.getExpression(), "Cronjob Express must not be null")));

            scheduledTasks.put(jobName, future);
            log.info("Scheduled job [{}] - cron [{}] - type [{}]", jobName, config.getExpression(),
                    config.getJobType());

        } catch (Exception e) {
            log.error("Failed to schedule job: " + config.getName(), e);
        }
    }

    @Override
    public boolean isJobEnabled(String jobName) {
        String key = CRON_STATUS_PREFIX + jobName;
        String status = redisService.getString(key);

        return status == null || Boolean.parseBoolean(status);
    }

    @Override
    public void setJobStatus(String jobName, boolean enabled) {
        String key = CRON_STATUS_PREFIX + jobName;
        redisService.setString(key, String.valueOf(enabled));
        log.info("Manual OP: set cronjob '{}' status to {}", jobName, enabled);

        refreshJobs();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("System is ready. Starting initial cronjob refresh...");
        this.refreshJobs();
    }
}
