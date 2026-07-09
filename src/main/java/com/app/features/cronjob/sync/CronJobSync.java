package com.app.features.cronjob.sync;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.constant.AppConstants;
import com.app.core.sync.SyncableDataService;
import com.app.features.cronjob.annotation.CronJobDef;
import com.app.features.cronjob.entity.CronJobConfigEntity;
import com.app.features.cronjob.enums.CronjobStatusEnum;
import com.app.features.cronjob.repository.CronJobConfigRepository;
import com.app.features.cronjob.scheduler.JobHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CronJobSync implements SyncableDataService {

    private final CronJobConfigRepository cronJobRepo;
    private final ApplicationContext applicationContext;

    @Override
    public String getSyncType() {
        return "SYNC_CRONJOB_CONFIGS";
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncToDatabase() {
        log.info("[Sync] Scanning for Cronjob defination...");

        List<CronJobConfigEntity> existingJobs = cronJobRepo.findAll();

        Map<String, CronJobConfigEntity> existingJobMap = existingJobs.stream()
                .collect(Collectors.toMap(CronJobConfigEntity::getJobType, Function.identity()));

        int insertCount = 0;
        int updateCount = 0;

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(CronJobDef.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(AppConstants.BASE_PACKAGE)) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                CronJobDef def = clazz.getAnnotation(CronJobDef.class);

                if (def == null) {
                    log.warn("[Sync] Annotation is null for class {}. Skipping.", clazz.getSimpleName());
                    continue;
                }

                JobHandler handler = (JobHandler) applicationContext.getBean(clazz);
                String jobType = handler.getSupportedJobType();

                if (existingJobMap.containsKey(jobType)) {
                    CronJobConfigEntity existingCron = existingJobMap.get(jobType);
                    boolean isChanged = false;

                    if (!existingCron.getExpression().equals(def.cronExpression())) {
                        existingCron.setExpression(def.cronExpression());
                        isChanged = true;
                    }
                    if (!existingCron.getLockAtLeastFor().equals(def.lockAtLeastFor())) {
                        existingCron.setLockAtLeastFor(def.lockAtLeastFor());
                        isChanged = true;
                    }
                    if (!existingCron.getLockAtMostFor().equals(def.lockAtMostFor())) {
                        existingCron.setLockAtMostFor(def.lockAtMostFor());
                        isChanged = true;
                    }

                    if (isChanged) {
                        updateCount++;
                        log.debug("[Sync] Updated definition for job: [{}]", jobType);
                    }
                } else {
                    CronJobConfigEntity cronConfig = new CronJobConfigEntity();
                    cronConfig.setName(def.jobName());
                    cronConfig.setExpression(def.cronExpression());
                    cronConfig.setJobType(jobType);
                    cronConfig.setLockAtLeastFor(def.lockAtLeastFor());
                    cronConfig.setLockAtMostFor(def.lockAtMostFor());
                    cronConfig.setStatus(CronjobStatusEnum.ACTIVE);

                    cronJobRepo.save(cronConfig);

                    log.info("[Sync] Seeded new Cronjob: [{}] with cron: [{}]", jobType, def.cronExpression());
                    insertCount++;
                }
            } catch (Exception e) {
                log.error("[Sync] Error processing bean definition: {}", bd.getBeanClassName(), e);
            }
        }

        log.info(">>> Sync [{}] COMPLETE. Inserted: {}, updated: {}", getSyncType(), insertCount, updateCount);
    }
}
