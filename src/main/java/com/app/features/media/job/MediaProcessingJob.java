package com.app.features.media.job;

import java.util.UUID;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.media.processing.MediaProcessor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaProcessingJob {

    private final MediaProcessor mediaProcessor;

    @Job(name = "Process uploaded media", retries = 3)
    public void execute(UUID mediaId) {
        mediaProcessor.process(mediaId);
    }
}
