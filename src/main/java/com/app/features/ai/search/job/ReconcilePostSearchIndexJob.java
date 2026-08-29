package com.app.features.ai.search.job;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.search.schema.model.PostSearchReconciliationResult;
import com.app.features.ai.search.service.PostSearchReconciliationService;
import com.app.features.cronjob.annotation.AppRecurringJob;
import com.app.features.cronjob.scheduler.JobHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@AppRecurringJob(
        id = "RECONCILE_POST_SEARCH_INDEX",
        name = "Reconcile Post Search Index",
        defaultCron = "0 */15 * * * *")
public class ReconcilePostSearchIndexJob implements JobHandler {

    private final PostSearchReconciliationService
            postSearchReconciliationSvc;

    @Override
    @Job(name = "Reconcile Post Search Index", retries = 3)
    public void execute() {
        PostSearchReconciliationResult result =
                postSearchReconciliationSvc.reconcile();
        if (result.availability() != AiAvailability.READY) {
            if (result.availability() == AiAvailability.DISABLED) {
                log.debug("Skipped disabled post search reconciliation.");
            } else {
                log.warn(
                        "Skipped post search reconciliation because search is unavailable.");
            }
            return;
        }

        log.info(
                "Post search reconciliation completed: recovery candidates [{}], backfill states created [{}], jobs enqueued [{}], enqueue failures [{}].",
                result.recoveryCandidates(),
                result.backfillStatesCreated(),
                result.jobsEnqueued(),
                result.failures());
    }
}
