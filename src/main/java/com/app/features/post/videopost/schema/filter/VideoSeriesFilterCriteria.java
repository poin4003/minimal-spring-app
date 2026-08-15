package com.app.features.post.videopost.schema.filter;

import java.util.UUID;

import com.app.features.post.videopost.enums.VideoSeriesLifecycleStatus;

import lombok.Data;

@Data
public class VideoSeriesFilterCriteria {

    private UUID ownerId;

    private String title;

    private VideoSeriesLifecycleStatus lifecycleStatus;
}
