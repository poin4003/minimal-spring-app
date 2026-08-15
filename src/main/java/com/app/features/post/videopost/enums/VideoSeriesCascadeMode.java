package com.app.features.post.videopost.enums;

public enum VideoSeriesCascadeMode {
    SERIES_ONLY,
    INCLUDE_VIDEOS;

    public boolean includesVideos() {
        return this == INCLUDE_VIDEOS;
    }
}
