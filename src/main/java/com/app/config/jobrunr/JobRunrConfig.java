package com.app.config.jobrunr;

import javax.sql.DataSource;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobRunrConfig {

    @Bean
    public StorageProvider storageProvider(DataSource dataSource) {
        return SqlStorageProviderFactory.using(dataSource);
    }

    @Bean
    public JobScheduler jobScheduler(StorageProvider storageProvider, ApplicationContext applicationContext) {
        return JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useJobActivator(applicationContext::getBean)
                .useBackgroundJobServer()
                .useDashboard()
                .initialize()
                .getJobScheduler();
    }
}