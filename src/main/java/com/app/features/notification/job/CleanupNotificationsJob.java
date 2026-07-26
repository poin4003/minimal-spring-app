package com.app.features.notification.job;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.cronjob.annotation.AppRecurringJob;
import com.app.features.cronjob.scheduler.JobHandler;
import com.app.features.notification.schema.model.NotificationCleanupResult;
import com.app.features.notification.service.NotificationMaintenanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@AppRecurringJob(
        id = "CLEANUP_NOTIFICATIONS",
        name = "Cleanup Notifications",
        defaultCron = "0 0 3 * * *")
public class CleanupNotificationsJob implements JobHandler {

    private final NotificationMaintenanceService notificationMaintenanceSvc;

    @Override
    @Job(name = "Cleanup Notifications", retries = 3)
    public void execute() {
        NotificationCleanupResult result =
                notificationMaintenanceSvc.cleanupNotifications();

        log.info(
                "Notification cleanup completed: expired [{}], overflow [{}].",
                result.getExpiredCount(),
                result.getOverflowCount());
    }
}
