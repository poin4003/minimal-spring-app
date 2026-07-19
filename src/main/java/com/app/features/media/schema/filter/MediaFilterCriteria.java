package com.app.features.media.schema.filter;

import java.util.UUID;

import org.springframework.util.StringUtils;

import com.app.core.enums.RecordStatus;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MediaFilterCriteria {

    @Size(max = 255)
    private String originalName;

    @Size(max = 64)
    private String publicKey;

    private UUID createdById;

    @Size(max = 255)
    private String createdByEmail;

    private MediaKind kind;

    private MediaProcessingStatus processingStatus;

    private RecordStatus status;

    public boolean hasOriginalName() {
        return StringUtils.hasText(originalName);
    }

    public boolean hasPublicKey() {
        return StringUtils.hasText(publicKey);
    }

    public boolean hasCreatedByEmail() {
        return StringUtils.hasText(createdByEmail);
    }
}
