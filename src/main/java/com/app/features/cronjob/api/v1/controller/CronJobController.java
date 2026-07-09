package com.app.features.cronjob.api.v1.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.core.response.ApiResult;
import com.app.features.cronjob.entity.CronJobConfigEntity;
import com.app.features.cronjob.repository.CronJobConfigRepository;
import com.app.features.cronjob.service.CronJobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/cronjobs")
@Tag(name = "Cronjob Management V1", description = "Manage dynamic cronjobs")
public class CronJobController {

    private final CronJobService cronJobService;
    private final CronJobConfigRepository cronJobConfigRepository;

    @GetMapping("")
    @Operation(summary = "Get all cronjob configs")
    public ApiResult<List<CronJobConfigEntity>> getAllCronjobs() {
        List<CronJobConfigEntity> configs = cronJobConfigRepository.findAll();
        log.info("Fetched {} cronjob configs", configs.size());
        return ApiResult.ok(configs, "Get cronjob configs success");
    }

    @GetMapping("/{jobName}/status")
    @Operation(summary = "Get cronjob enable status from Redis")
    public ApiResult<Boolean> getCronjobStatus(@PathVariable String jobName) {
        boolean enabled = cronJobService.isJobEnabled(jobName);
        return ApiResult.ok(enabled, "Get cronjob status success");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh cronjobs from database and reschedule")
    public ApiResult<Void> refreshCronjobs() {
        cronJobService.refreshJobs();
        log.info("Manual refresh of cronjobs triggered");
        return ApiResult.ok(null, "Refresh cronjobs success");
    }

    @PostMapping("/{jobName}/enable")
    @Operation(summary = "Enable a cronjob (Redis flag) and refresh schedules")
    public ApiResult<Void> enableCronjob(@PathVariable String jobName) {
        cronJobService.setJobStatus(jobName, true);
        log.info("Enabled cronjob '{}'", jobName);
        return ApiResult.ok(null, "Enable cronjob success");
    }

    @PostMapping("/{jobName}/disable")
    @Operation(summary = "Disable a cronjob (Redis flag) and refresh schedules")
    public ApiResult<Void> disableCronjob(@PathVariable String jobName) {
        cronJobService.setJobStatus(jobName, false);
        log.info("Disabled cronjob '{}'", jobName);
        return ApiResult.ok(null, "Disable cronjob success");
    }
}
