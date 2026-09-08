package com.app.features.post.catalogpost.mapper;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.features.post.catalogpost.entity.CatalogAttributeEntity;
import com.app.features.post.catalogpost.entity.CatalogAttributeOptionEntity;
import com.app.features.post.catalogpost.entity.CatalogCategoryAttributeEntity;
import com.app.features.post.catalogpost.entity.CatalogCategoryEntity;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeDefinitionResult;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeOptionResult;
import com.app.features.post.catalogpost.schema.result.CatalogCategoryAttributeResult;
import com.app.features.post.catalogpost.schema.result.CatalogCategoryResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CatalogConfigurationMapper {

    private final ModelMapper mapper;

    public CatalogCategoryResult toCategoryResult(
            CatalogCategoryEntity category) {
        CatalogCategoryResult result = mapper.map(
                category,
                CatalogCategoryResult.class);
        CatalogCategoryEntity parent = category.getParent();
        if (parent != null) {
            result.setParentId(parent.getId());
            result.setParentName(parent.getName());
        }
        return result;
    }

    public CatalogAttributeDefinitionResult toAttributeResult(
            CatalogAttributeEntity attribute) {
        return toAttributeResult(attribute, List.of());
    }

    public CatalogAttributeDefinitionResult toAttributeResult(
            CatalogAttributeEntity attribute,
            List<CatalogAttributeOptionResult> options) {
        CatalogAttributeDefinitionResult result = mapper.map(
                attribute,
                CatalogAttributeDefinitionResult.class);
        result.setOptions(options);
        return result;
    }

    public CatalogAttributeOptionResult toOptionResult(
            CatalogAttributeOptionEntity option) {
        CatalogAttributeOptionResult result = mapper.map(
                option,
                CatalogAttributeOptionResult.class);
        result.setAttributeId(option.getAttribute().getId());
        return result;
    }

    public CatalogCategoryAttributeResult toCategoryAttributeResult(
            CatalogCategoryAttributeEntity mapping,
            CatalogAttributeDefinitionResult attribute) {
        CatalogCategoryAttributeResult result = mapper.map(
                mapping,
                CatalogCategoryAttributeResult.class);
        result.setAttribute(attribute);
        return result;
    }
}
