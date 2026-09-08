package com.app.features.post.catalogpost.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.core.enums.RecordStatus;
import com.app.features.post.catalogpost.entity.CatalogAttributeEntity;
import com.app.features.post.catalogpost.schema.filter.CatalogAttributeFilterCriteria;
import com.app.features.post.catalogpost.schema.payload.CreateCatalogAttributeOptionPayload;
import com.app.features.post.catalogpost.schema.payload.CreateCatalogAttributePayload;
import com.app.features.post.catalogpost.schema.payload.UpdateCatalogAttributeOptionPayload;
import com.app.features.post.catalogpost.schema.payload.UpdateCatalogAttributePayload;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeDefinitionResult;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeOptionResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface CatalogAttributeService {

    CatalogAttributeDefinitionResult createAttribute(
            @NotNull @Valid CreateCatalogAttributePayload payload);

    CatalogAttributeDefinitionResult updateAttribute(
            @NotNull UUID attributeId,
            @NotNull @Valid UpdateCatalogAttributePayload payload);

    void updateAttributeStatus(
            @NotNull UUID attributeId,
            @NotNull RecordStatus status);

    CatalogAttributeDefinitionResult getAttribute(@NotNull UUID attributeId);

    Page<CatalogAttributeDefinitionResult> getAttributes(
            @NotNull CatalogAttributeFilterCriteria criteria,
            @NotNull Pageable pageable);

    CatalogAttributeOptionResult createOption(
            @NotNull UUID attributeId,
            @NotNull @Valid CreateCatalogAttributeOptionPayload payload);

    CatalogAttributeOptionResult updateOption(
            @NotNull UUID attributeId,
            @NotNull UUID optionId,
            @NotNull @Valid UpdateCatalogAttributeOptionPayload payload);

    void updateOptionStatus(
            @NotNull UUID attributeId,
            @NotNull UUID optionId,
            @NotNull RecordStatus status);

    Page<CatalogAttributeOptionResult> getOptions(
            @NotNull UUID attributeId,
            @NotNull Pageable pageable);

    CatalogAttributeOptionResult getOption(
            @NotNull UUID attributeId,
            @NotNull UUID optionId);

    CatalogAttributeEntity requireAttribute(@NotNull UUID attributeId);

    CatalogAttributeEntity requireActiveAttribute(@NotNull UUID attributeId);
}
