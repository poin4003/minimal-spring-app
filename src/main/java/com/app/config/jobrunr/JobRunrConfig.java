package com.app.config.jobrunr;

import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

import javax.sql.DataSource;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JobRunrConfig {

    private final JobRunrProperties jobRunrProperties;

    @Bean
    public StorageProvider storageProvider(DataSource dataSource) {
        return SqlStorageProviderFactory.using(dataSource);
    }

    @Bean
    public JobScheduler jobScheduler(StorageProvider storageProvider, ApplicationContext applicationContext) {
        JobRunrProperties.BackgroundJobServer backgroundJobServer =
                jobRunrProperties.getBackgroundJobServer();
        JobRunrProperties.Dashboard dashboard = jobRunrProperties.getDashboard();

        BackgroundJobServerConfiguration serverConfiguration =
                usingStandardBackgroundJobServerConfiguration()
                        .andWorkerCount(backgroundJobServer.getWorkerCount())
                        .andDeleteSucceededJobsAfter(
                                backgroundJobServer.getDeleteSucceededJobsAfter())
                        .andPermanentlyDeleteDeletedJobsAfter(
                                backgroundJobServer.getPermanentlyDeleteDeletedJobsAfter());

        return JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useJobActivator(new SpringJobActivator(applicationContext))
                .useBackgroundJobServerIf(
                        backgroundJobServer.isEnabled(),
                        serverConfiguration)
                .useDashboardIf(dashboard.isEnabled(), dashboard.getPort())
                .initialize()
                .getJobScheduler();
    }
}
