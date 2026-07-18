package com.app.features.media.schema.filter;

import org.springframework.util.StringUtils;

import com.app.core.enums.RecordStatus;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MediaFilterCriteria {

    @Size(max = 255)
    private String originalName;

    @Size(max = 64)
    private String publicKey;

    @Size(max = 255)
    private String createdByEmail;

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
