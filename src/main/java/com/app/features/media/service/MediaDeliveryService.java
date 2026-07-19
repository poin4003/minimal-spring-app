package com.app.features.media.service;

import com.app.features.media.schema.result.MediaDeliveryResult;

public interface MediaDeliveryService {

    MediaDeliveryResult getOriginal(String publicKey);

    MediaDeliveryResult getHlsManifest(String publicKey);

    MediaDeliveryResult getHlsRendition(String publicKey, String variantKey);

    MediaDeliveryResult getHlsSegment(
            String publicKey,
            String variantKey,
            String segmentName);
}
