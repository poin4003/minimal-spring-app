package com.app.features.media.enums;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HlsVariantProfile {
    MASTER("master", null, 0, 0),
    AUDIO("audio", null, 0, 192_000),
    VIDEO_360P("360p", 360, 800_000, 96_000),
    VIDEO_720P("720p", 720, 2_800_000, 128_000),
    VIDEO_1080P("1080p", 1080, 5_000_000, 192_000);

    private static final List<HlsVariantProfile> VIDEO_RENDITIONS = List.of(
            VIDEO_360P,
            VIDEO_720P,
            VIDEO_1080P);

    private final String key;
    private final Integer height;
    private final int videoBitrate;
    private final int audioBitrate;

    public int getTotalBitrate() {
        return videoBitrate + audioBitrate;
    }

    public static List<HlsVariantProfile> getVideoRenditions() {
        return VIDEO_RENDITIONS;
    }
}
