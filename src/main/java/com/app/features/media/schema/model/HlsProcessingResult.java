package com.app.features.media.schema.model;

import java.util.List;

import lombok.Getter;

@Getter
public class HlsProcessingResult {

    private final String publishedDirectoryKey;

    private final List<HlsEncodingProfile> profiles;

    public HlsProcessingResult(
            String publishedDirectoryKey,
            List<HlsEncodingProfile> profiles) {
        this.publishedDirectoryKey = publishedDirectoryKey;
        this.profiles = List.copyOf(profiles);
    }
}
