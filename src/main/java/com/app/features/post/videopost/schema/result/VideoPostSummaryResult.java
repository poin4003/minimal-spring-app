package com.app.features.post.videopost.schema.result;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.post.schema.result.PostMediaResult;

import lombok.Data;

@Data
public class VideoPostSummaryResult {

    private UUID id;

    private String title;

    private LocalDateTime publishedAt;

    private PostMediaResult content;
}
