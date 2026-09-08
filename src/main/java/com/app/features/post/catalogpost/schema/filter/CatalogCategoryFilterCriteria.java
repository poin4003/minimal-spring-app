package com.app.features.post.catalogpost.schema.filter;

import java.util.UUID;

import com.app.core.enums.RecordStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CatalogCategoryFilterCriteria {

    private String text;
    private RecordStatus status;
    private UUID parentId;
}
