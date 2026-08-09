package com.app.features.post.job;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.cronjob.annotation.AppRecurringJob;
import com.app.features.cronjob.scheduler.JobHandler;
import com.app.features.post.schema.model.PostCleanupResult;
import com.app.features.post.service.PostMaintenanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@AppRecurringJob(
        id = "CLEANUP_POSTS",
        name = "Cleanup Posts",
        defaultCron = "0 30 3 * * *")
public class CleanupPostsJob implements JobHandler {

    private final PostMaintenanceService postMaintenanceSvc;

    @Override
    @Job(name = "Cleanup Posts", retries = 3)
    public void execute() {
        PostCleanupResult result = postMaintenanceSvc.cleanupExpiredPosts();

        log.info(
                "Post cleanup completed: deleted [{}], rejected [{}].",
                result.getDeletedCount(),
                result.getRejectedCount());
    }
}
