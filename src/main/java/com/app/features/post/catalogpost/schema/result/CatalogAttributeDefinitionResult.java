package com.app.features.post.catalogpost.schema.result;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.app.core.enums.RecordStatus;
import com.app.features.post.catalogpost.enums.CatalogAttributeValueType;

import lombok.Data;

@Data
public class CatalogAttributeDefinitionResult {

    private UUID id;
    private String key;
    private String name;
    private String description;
    private CatalogAttributeValueType valueType;
    private String unit;
    private boolean searchable;
    private RecordStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<CatalogAttributeOptionResult> options = List.of();
}
