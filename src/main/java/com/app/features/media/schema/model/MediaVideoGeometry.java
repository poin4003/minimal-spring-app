package com.app.features.media.schema.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MediaVideoGeometry {

    private final int width;

    private final int height;

    private final int rotation;

    public boolean hasRotation() {
        return rotation != 0;
    }
}
