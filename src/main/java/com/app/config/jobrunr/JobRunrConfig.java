package com.app.config.jobrunr;

import javax.sql.DataSource;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobRunrConfig {

    @Value("${org.jobrunr.background-job-server.enabled:true}")
    private boolean backgroundJobServerEnabled;

    @Value("${org.jobrunr.background-job-server.worker-count:2}")
    private int workerCount;

    @Value("${org.jobrunr.dashboard.enabled:false}")
    private boolean dashboardEnabled;

    @Value("${org.jobrunr.dashboard.port:8000}")
    private int dashboardPort;

    @Bean
    public StorageProvider storageProvider(DataSource dataSource) {
        return SqlStorageProviderFactory.using(dataSource);
    }

    @Bean
    public JobScheduler jobScheduler(StorageProvider storageProvider, ApplicationContext applicationContext) {
        return JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useJobActivator(new SpringJobActivator(applicationContext))
                .useBackgroundJobServerIf(backgroundJobServerEnabled, workerCount)
                .useDashboardIf(dashboardEnabled, dashboardPort)
                .initialize()
                .getJobScheduler();
    }
}
