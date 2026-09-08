package com.app.features.post.catalogpost.schema.result;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.core.enums.RecordStatus;

import lombok.Data;

@Data
public class CatalogAttributeOptionResult {

    private UUID id;
    private UUID attributeId;
    private String code;
    private String label;
    private int sortOrder;
    private RecordStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
