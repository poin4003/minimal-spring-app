package com.app.features.cronjob.scheduler;

public interface JobHandler {
    
    void execute();

    String getSupportedJobType();
}
