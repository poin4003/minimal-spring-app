package com.app.features.cronjob.scheduler;

import java.time.ZoneId;
import java.util.Collections;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.cron.CronExpression;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecurringJobDefinition {

    private final String id;
    private final String name;
    private final String defaultCron;
    private final String zoneId;
    private final Class<? extends JobHandler> handlerClass;

    public String resolveCron(String overrideCron) {
        return overrideCron == null || overrideCron.isBlank()
                ? defaultCron
                : overrideCron;
    }

    public RecurringJob toRecurringJob(String cron) {
        JobDetails jobDetails = new JobDetails(
                handlerClass.getName(),
                null,
                "execute",
                Collections.emptyList());

        RecurringJob recurringJob = new RecurringJob(
                id,
                jobDetails,
                CronExpression.create(cron),
                ZoneId.of(zoneId));

        recurringJob.setJobName(name);
        return recurringJob;
    }
}
