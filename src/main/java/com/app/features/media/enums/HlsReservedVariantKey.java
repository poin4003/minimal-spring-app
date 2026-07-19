package com.app.features.media.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HlsReservedVariantKey {
    MASTER("master"),
    AUDIO("audio");

    private final String key;
}
