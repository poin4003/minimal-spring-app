package com.app.features.media.api.v1.support;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.media.schema.result.MediaDeliveryResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaResponseFactory {

    private final AppProperties appProperties;

    public ResponseEntity<Resource> build(MediaDeliveryResult result) {
        FileSystemResource resource = new FileSystemResource(result.getPath());
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .cacheControl(CacheControl
                        .maxAge(appProperties.getMedia().getDeliveryCacheDuration())
                        .cachePublic()
                        .immutable())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header("X-Content-Type-Options", "nosniff");

        if (result.getFileName() != null) {
            ContentDisposition disposition = result.isAttachment()
                    ? ContentDisposition.attachment()
                            .filename(result.getFileName(), StandardCharsets.UTF_8)
                            .build()
                    : ContentDisposition.inline()
                            .filename(result.getFileName(), StandardCharsets.UTF_8)
                            .build();
            response.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        }

        return response.body(resource);
    }
}
