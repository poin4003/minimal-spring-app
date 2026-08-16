package com.app.features.post.videopost.schema.result;

import java.util.UUID;

import lombok.Data;

@Data
public class VideoSeriesItemResult {

    private UUID id;

    private int position;

    private VideoPostSummaryResult video;
}
