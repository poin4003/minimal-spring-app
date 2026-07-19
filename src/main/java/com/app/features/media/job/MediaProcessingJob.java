package com.app.features.media.job;

import java.util.UUID;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.springframework.stereotype.Component;

import com.app.features.media.service.MediaProcessingService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaProcessingJob {

    private final MediaProcessingService mediaProcessingService;

    @Job(name = "Process uploaded media", retries = 3)
    public void execute(UUID mediaId, JobContext jobContext) {
        mediaProcessingService.process(mediaId, jobContext.getJobId());
    }
}
