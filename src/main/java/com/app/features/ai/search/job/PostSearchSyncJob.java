package com.app.features.ai.search.job;

import java.util.UUID;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.app.features.ai.search.service.PostSearchSyncService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.ai.search",
        name = "enabled",
        havingValue = "true")
public class PostSearchSyncJob {

    private final PostSearchSyncService postSearchSyncSvc;

    @Job(name = "Synchronize post with semantic search index", retries = 0)
    public void execute(UUID postId) {
        postSearchSyncSvc.synchronize(postId);
    }
}
