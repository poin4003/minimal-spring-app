package com.app.features.media.schema.result;

import java.util.UUID;

import com.app.features.media.enums.MediaKind;

import lombok.Data;

@Data
public class PublicMediaResult {
    
    private UUID id;

    private MediaKind kind;

    private String originalName;

    private String contentType;

    private Integer originalWidth;

    private Integer originalHeight;

    private String contentUrl;

    private String originalUrl;

    private String thumbnailUrl;
}
