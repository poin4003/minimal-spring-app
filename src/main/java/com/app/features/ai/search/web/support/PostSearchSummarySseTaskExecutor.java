package com.app.features.ai.search.web.support;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;

@Component
public class PostSearchSummarySseTaskExecutor
        extends ThreadPoolTaskExecutor {

    public PostSearchSummarySseTaskExecutor(
            AppProperties appProperties) {
        AppProperties.SearchSummaryStreamSettings streamSettings =
                appProperties.getAi().getSearch().getSummary().getStream();

        setCorePoolSize(streamSettings.getWorkers());
        setMaxPoolSize(streamSettings.getWorkers());
        setQueueCapacity(streamSettings.getQueueCapacity());
        setThreadNamePrefix("post-search-summary-sse-");
        setWaitForTasksToCompleteOnShutdown(false);
        setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    }
}
