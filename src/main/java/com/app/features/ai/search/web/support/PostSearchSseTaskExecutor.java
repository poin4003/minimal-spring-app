package com.app.features.ai.search.web.support;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;

@Component
public class PostSearchSseTaskExecutor extends ThreadPoolTaskExecutor {

    public PostSearchSseTaskExecutor(AppProperties appProperties) {
        AppProperties.RagStreamSettings streamSettings =
                appProperties.getAi().getRag().getStream();

        setCorePoolSize(streamSettings.getWorkers());
        setMaxPoolSize(streamSettings.getWorkers());
        setQueueCapacity(streamSettings.getQueueCapacity());
        setThreadNamePrefix("post-search-sse-");
        setWaitForTasksToCompleteOnShutdown(false);
        setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    }
}
