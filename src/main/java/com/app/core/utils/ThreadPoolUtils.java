package com.app.core.utils;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class ThreadPoolUtils {
    /**
     * Initialize and configure a ThreadPoolTaskExecutor for background tasks.
     * @param corePoolSize Minimum threads (worker) always available.
     * @param maxPoolSize Maxinum threads (worker) can be create.
     * @param queueCapacity Task queue capacity.
     * @param keepAliveSeconds Thread holding time exceeds corePoolSize after job completion. 
     * @param threadNamePrefix Thread name prefix 
     * @return ThreadPoolTaskExecutor has been initialized.
     */
    public static ThreadPoolTaskExecutor createThreadPoolTaskExecutor(
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity,
            int keepAliveSeconds,
            String threadNamePrefix) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity); 
        executor.setKeepAliveSeconds(keepAliveSeconds); 
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true); 
        executor.initialize();
        return executor;
    }
}
