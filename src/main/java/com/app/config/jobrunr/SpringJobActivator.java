package com.app.config.jobrunr;

import org.jobrunr.server.JobActivator;
import org.springframework.context.ApplicationContext;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SpringJobActivator implements JobActivator {

    private final ApplicationContext applicationContext;

    @Override
    public <T> T activateJob(Class<T> type) {
        return applicationContext.getBean(type);
    }
}
