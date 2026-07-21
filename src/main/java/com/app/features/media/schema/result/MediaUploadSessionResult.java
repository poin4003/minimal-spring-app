package com.app.features.media.schema.result;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.app.features.media.enums.MediaUploadStatus;

import lombok.Data;

@Data
public class MediaUploadSessionResult {

    private UUID id;

    private String originalName;

    private long fileSize;

    private int chunkSize;

    private int totalChunks;

    private List<Integer> uploadedChunks;

    private MediaUploadStatus status;

    private LocalDateTime expiresAt;

    private MediaResult completedMedia;
}
