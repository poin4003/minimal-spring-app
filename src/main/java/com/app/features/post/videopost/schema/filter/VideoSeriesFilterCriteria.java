package com.app.features.post.videopost.schema.filter;

import java.util.UUID;

import lombok.Data;

@Data
public class VideoSeriesFilterCriteria {

    private UUID ownerId;

    private String title;
}
