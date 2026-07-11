package com.app.features.cronjob.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.app.features.cronjob.entity.CronJobConfigEntity;
import com.app.features.cronjob.enums.CronjobStatusEnum;
import com.app.features.cronjob.repository.CronJobConfigRepository;
import com.app.features.cronjob.scheduler.JobHandler;
import com.app.features.cronjob.service.CronJobService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CronJobServiceImpl implements CronJobService {

    private final TaskScheduler taskScheduler;
    private final CronJobConfigRepository jobConfigRepo;
    private final Map<String, JobHandler> jobHandlerMap;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, Boolean> jobStatusMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> jobLocks = new ConcurrentHashMap<>();

    public CronJobServiceImpl(
            TaskScheduler taskScheduler,
            CronJobConfigRepository jobConfigRepo,
            List<JobHandler> jobHandlers) {

        this.taskScheduler = taskScheduler;
        this.jobConfigRepo = jobConfigRepo;

        this.jobHandlerMap = jobHandlers.stream()
                .collect(Collectors.toMap(jobHandler -> jobHandler.getSupportedJobType(), Function.identity()));
    }

    @Override
    public void refreshJobs() {
        log.info("Refreshing dynamic jobs from Database...");

        List<CronJobConfigEntity> configs = jobConfigRepo.findByStatus(CronjobStatusEnum.ACTIVE);

        scheduledTasks.forEach((k, v) -> {
            v.cancel(false);
            log.debug("Cancelled job: {}", k);
        });
        scheduledTasks.clear();
        jobLocks.clear();

        for (CronJobConfigEntity config : configs) {
            scheduleSingleJob(config);
        }

        log.info("Refreshed {} jobs successfully.", scheduledTasks.size());
    }

    private void scheduleSingleJob(CronJobConfigEntity config) {
        try {
            String jobName = config.getName();

            if (!isJobEnabled(jobName)) {
                log.warn("Job [{}] is DISABLED. Skipping.", jobName);
                return;
            }

            JobHandler handler = jobHandlerMap.get(config.getJobType());
            if (handler == null) {
                log.error("No Handler found for job type: [{}]. Skipping.", config.getJobType());
                return;
            }

            Runnable lockableTask = () -> {
                AtomicBoolean isRunning = jobLocks.computeIfAbsent(jobName, k -> new AtomicBoolean(false));

                if (isRunning.compareAndSet(false, true)) {
                    try {
                        log.info("Starting job: {}", jobName);
                        handler.execute();
                        log.info("Finished job: {}", jobName);
                    } catch (Exception e) {
                        log.error("Error executing job: {}", jobName, e);
                    } finally {
                        isRunning.set(false);
                    }
                } else {
                    log.warn("Job [{}] is still running. Skipping this execution tick.", jobName);
                }
            };

            ScheduledFuture<?> future = taskScheduler.schedule(
                    lockableTask,
                    new CronTrigger(
                            Objects.requireNonNull(config.getExpression(), "Cronjob Expression must not be null")));

            scheduledTasks.put(jobName, future);
            log.info("Scheduled job [{}] - cron [{}] - type [{}]", jobName, config.getExpression(),
                    config.getJobType());

        } catch (Exception e) {
            log.error("Failed to schedule job: " + config.getName(), e);
        }
    }

    @Override
    public boolean isJobEnabled(String jobName) {
        return jobStatusMap.getOrDefault(jobName, true);
    }

    @Override
    public void setJobStatus(String jobName, boolean enabled) {
        jobStatusMap.put(jobName, enabled);
        log.info("Manual OP: set cronjob '{}' status to {}", jobName, enabled);
        refreshJobs();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("System is ready. Starting initial cronjob refresh...");
        this.refreshJobs();
    }
}
