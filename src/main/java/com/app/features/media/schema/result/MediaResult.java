package com.app.features.media.schema.result;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.core.enums.RecordStatus;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.user.schema.result.UserShortResult;

import lombok.Data;

@Data
public class MediaResult {

    private UUID id;

    private UserShortResult createdBy;

    private String publicKey;

    private String originalName;

    private String contentType;

    private long fileSize;

    private Integer originalWidth;

    private Integer originalHeight;

    private String contentUrl;

    private String originalUrl;

    private String thumbnailUrl;

    private MediaKind kind;

    private MediaProcessingStatus processingStatus;

    private RecordStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
