package com.app.features.media.schema.result;

import java.util.UUID;

import com.app.features.media.enums.MediaVariantType;

import lombok.Data;

@Data
public class MediaVariantResult {

    private UUID id;

    private MediaVariantType variantType;

    private String variantKey;

    private String contentType;

    private Integer width;

    private Integer height;

    private Integer bitrate;

    private String createdAt;

    private String updatedAt;
}
