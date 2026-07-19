package com.app.features.media.schema.model;

import com.app.config.settings.AppProperties.HlsRendition;
import com.app.features.media.enums.HlsReservedVariantKey;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class HlsEncodingProfile {

    private final String key;
    private final Integer height;
    private final int videoBitrate;
    private final int audioBitrate;

    public static HlsEncodingProfile from(HlsRendition rendition) {
        return new HlsEncodingProfile(
                rendition.getKey(),
                rendition.getHeight(),
                rendition.getVideoBitrate(),
                rendition.getAudioBitrate());
    }

    public static HlsEncodingProfile audio(int audioBitrate) {
        return new HlsEncodingProfile(
                HlsReservedVariantKey.AUDIO.getKey(),
                null,
                0,
                audioBitrate);
    }

    public int getTotalBitrate() {
        return videoBitrate + audioBitrate;
    }

    public boolean isVideo() {
        return height != null;
    }
}
