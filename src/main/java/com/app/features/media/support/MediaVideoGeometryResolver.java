package com.app.features.media.support;

import java.util.List;

import org.springframework.stereotype.Component;

import com.app.features.media.schema.model.MediaVideoGeometry;
import com.github.kokorin.jaffree.ffprobe.SideData;
import com.github.kokorin.jaffree.ffprobe.Stream;

@Component
public class MediaVideoGeometryResolver {

    private static final int FULL_ROTATION_DEGREES = 360;
    private static final int QUARTER_TURN_DEGREES = 90;
    private static final int THREE_QUARTER_TURN_DEGREES = 270;

    public MediaVideoGeometry resolve(Stream stream) {
        int rotation = resolveRotation(stream);
        boolean swapsDimensions = rotation == QUARTER_TURN_DEGREES
                || rotation == THREE_QUARTER_TURN_DEGREES;

        return new MediaVideoGeometry(
                swapsDimensions ? stream.getHeight() : stream.getWidth(),
                swapsDimensions ? stream.getWidth() : stream.getHeight(),
                rotation);
    }

    private int resolveRotation(Stream stream) {
        List<SideData> sideData = stream.getSideDataList();
        if (sideData == null) {
            return 0;
        }

        return sideData.stream()
                .map(item -> item.getRotation())
                .filter(rotation -> rotation != null)
                .findFirst()
                .map(rotation -> Math.floorMod(
                        rotation,
                        FULL_ROTATION_DEGREES))
                .orElse(0);
    }
}
