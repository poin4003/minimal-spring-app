package com.app.features.post.videopost.schema.result;

import java.util.List;

import lombok.Data;

@Data
public class VideoSeriesDetailResult {

    private VideoSeriesResult series;

    private List<VideoSeriesItemResult> items = List.of();
}
