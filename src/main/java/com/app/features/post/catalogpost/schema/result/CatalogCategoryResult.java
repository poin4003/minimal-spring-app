package com.app.features.post.catalogpost.schema.result;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.core.enums.RecordStatus;

import lombok.Data;

@Data
public class CatalogCategoryResult {

    private UUID id;
    private UUID parentId;
    private String parentName;
    private String name;
    private String slug;
    private String description;
    private int sortOrder;
    private RecordStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
