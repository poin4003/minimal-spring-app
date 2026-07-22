package com.app.features.media.api.v1.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.features.media.api.v1.support.MediaResponseFactory;
import com.app.features.media.schema.result.MediaDeliveryResult;
import com.app.features.media.service.MediaDeliveryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/media")
public class PublicMediaApiController {

    private final MediaDeliveryService mediaDeliverySvc;
    private final MediaResponseFactory mediaResponseFactory;

    @GetMapping("/{publicKey}")
    public ResponseEntity<Resource> getOriginal(@PathVariable String publicKey) {
        MediaDeliveryResult result = mediaDeliverySvc.getOriginal(publicKey);
        return mediaResponseFactory.build(result);
    }

    @GetMapping("/{publicKey}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String publicKey) {
        MediaDeliveryResult result = mediaDeliverySvc.getThumbnail(publicKey);
        return mediaResponseFactory.build(result);
    }

    @GetMapping("/{publicKey}/hls/index.m3u8")
    public ResponseEntity<Resource> getHlsManifest(@PathVariable String publicKey) {
        MediaDeliveryResult result = mediaDeliverySvc.getHlsManifest(publicKey);
        return mediaResponseFactory.build(result);
    }

    @GetMapping("/{publicKey}/hls/{variantKey}/index.m3u8")
    public ResponseEntity<Resource> getHlsRendition(
            @PathVariable String publicKey,
            @PathVariable String variantKey) {
        MediaDeliveryResult result = mediaDeliverySvc.getHlsRendition(
                publicKey,
                variantKey);
        return mediaResponseFactory.build(result);
    }

    @GetMapping("/{publicKey}/hls/{variantKey}/{segmentName}")
    public ResponseEntity<Resource> getHlsSegment(
            @PathVariable String publicKey,
            @PathVariable String variantKey,
            @PathVariable String segmentName) {
        MediaDeliveryResult result = mediaDeliverySvc.getHlsSegment(
                publicKey,
                variantKey,
                segmentName);
        return mediaResponseFactory.build(result);
    }
}
