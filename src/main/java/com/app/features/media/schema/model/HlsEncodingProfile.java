package com.app.features.media.schema.model;

import com.app.config.settings.AppProperties.HlsRendition;
import com.app.features.media.enums.HlsReservedVariantKey;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class HlsEncodingProfile {

    private final String key;
    private final Integer width;
    private final Integer height;
    private final int videoBitrate;
    private final int audioBitrate;

    public static HlsEncodingProfile from(
            HlsRendition rendition,
            int sourceWidth,
            int sourceHeight) {
        int sourceShortEdge = Math.min(sourceWidth, sourceHeight);
        int targetShortEdge = Math.min(
                rendition.getShortEdge(),
                sourceShortEdge);
        double scale = (double) targetShortEdge / sourceShortEdge;
        int scaledWidth = toEvenDimension(sourceWidth * scale);
        int scaledHeight = toEvenDimension(sourceHeight * scale);

        return new HlsEncodingProfile(
                rendition.getKey(),
                scaledWidth,
                scaledHeight,
                rendition.getVideoBitrate(),
                rendition.getAudioBitrate());
    }

    private static int toEvenDimension(double dimension) {
        int rounded = (int) Math.round(dimension);
        int even = rounded % 2 == 0
                ? rounded
                : rounded + 1;
        return Math.max(2, even);
    }

    public static HlsEncodingProfile audio(int audioBitrate) {
        return new HlsEncodingProfile(
                HlsReservedVariantKey.AUDIO.getKey(),
                null,
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
