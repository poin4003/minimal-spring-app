package com.app.features.post.catalogpost.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.core.enums.RecordStatus;
import com.app.features.post.catalogpost.entity.CatalogCategoryEntity;
import com.app.features.post.catalogpost.schema.filter.CatalogCategoryFilterCriteria;
import com.app.features.post.catalogpost.schema.payload.CreateCatalogCategoryPayload;
import com.app.features.post.catalogpost.schema.payload.ReplaceCatalogCategoryAttributesPayload;
import com.app.features.post.catalogpost.schema.payload.UpdateCatalogCategoryPayload;
import com.app.features.post.catalogpost.schema.result.CatalogCategoryFormSchemaResult;
import com.app.features.post.catalogpost.schema.result.CatalogCategoryResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface CatalogCategoryService {

    CatalogCategoryResult createCategory(
            @NotNull @Valid CreateCatalogCategoryPayload payload);

    CatalogCategoryResult updateCategory(
            @NotNull UUID categoryId,
            @NotNull @Valid UpdateCatalogCategoryPayload payload);

    void updateCategoryStatus(
            @NotNull UUID categoryId,
            @NotNull RecordStatus status);

    CatalogCategoryResult getCategory(@NotNull UUID categoryId);

    Page<CatalogCategoryResult> getCategories(
            @NotNull CatalogCategoryFilterCriteria criteria,
            @NotNull Pageable pageable);

    CatalogCategoryFormSchemaResult replaceCategoryAttributes(
            @NotNull UUID categoryId,
            @NotNull @Valid ReplaceCatalogCategoryAttributesPayload payload);

    CatalogCategoryFormSchemaResult updateCategoryAttributeSelection(
            @NotNull UUID categoryId,
            @NotNull List<UUID> managedAttributeIds,
            @NotNull @Valid ReplaceCatalogCategoryAttributesPayload payload);

    CatalogCategoryFormSchemaResult getCategoryConfiguration(
            @NotNull UUID categoryId);

    CatalogCategoryFormSchemaResult getActiveFormSchema(
            @NotNull UUID categoryId);

    CatalogCategoryEntity requireCategory(@NotNull UUID categoryId);

    CatalogCategoryEntity requireActiveCategory(@NotNull UUID categoryId);
}
