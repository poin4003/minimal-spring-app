package com.app.features.post.aimoderation.job;

import java.util.UUID;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.post.aimoderation.service.PostAiModerationWorkflowService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostAiModerationJob {

    private final PostAiModerationWorkflowService postAiModerationWorkflowSvc;

    @Job(name = "Moderate submitted post with AI", retries = 0)
    public void execute(UUID postId) {
        postAiModerationWorkflowSvc.moderate(postId);
    }
}
