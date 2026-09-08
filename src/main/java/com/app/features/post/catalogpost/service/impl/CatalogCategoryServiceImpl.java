package com.app.features.post.catalogpost.service.impl;

import static com.app.core.utils.TextNormalizationUtils.normalizeIdentifier;
import static com.app.core.utils.TextNormalizationUtils.normalizeOptional;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.enums.RecordStatus;
import com.app.core.exception.ExceptionFactory;
import com.app.features.post.catalogpost.entity.CatalogAttributeEntity;
import com.app.features.post.catalogpost.entity.CatalogAttributeOptionEntity;
import com.app.features.post.catalogpost.entity.CatalogCategoryAttributeEntity;
import com.app.features.post.catalogpost.entity.CatalogCategoryEntity;
import com.app.features.post.catalogpost.enums.CatalogAttributeValueType;
import com.app.features.post.catalogpost.mapper.CatalogConfigurationMapper;
import com.app.features.post.catalogpost.repository.CatalogAttributeOptionRepository;
import com.app.features.post.catalogpost.repository.CatalogAttributeRepository;
import com.app.features.post.catalogpost.repository.CatalogCategoryAttributeRepository;
import com.app.features.post.catalogpost.repository.CatalogCategoryRepository;
import com.app.features.post.catalogpost.repository.spec.CatalogCategorySpecification;
import com.app.features.post.catalogpost.schema.filter.CatalogCategoryFilterCriteria;
import com.app.features.post.catalogpost.schema.payload.CatalogCategoryAttributePayload;
import com.app.features.post.catalogpost.schema.payload.CreateCatalogCategoryPayload;
import com.app.features.post.catalogpost.schema.payload.ReplaceCatalogCategoryAttributesPayload;
import com.app.features.post.catalogpost.schema.payload.UpdateCatalogCategoryPayload;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeDefinitionResult;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeOptionResult;
import com.app.features.post.catalogpost.schema.result.CatalogCategoryAttributeResult;
import com.app.features.post.catalogpost.schema.result.CatalogCategoryFormSchemaResult;
import com.app.features.post.catalogpost.schema.result.CatalogCategoryResult;
import com.app.features.post.catalogpost.service.CatalogCategoryService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogCategoryServiceImpl implements CatalogCategoryService {

    private final CatalogCategoryRepository categoryRepo;
    private final CatalogAttributeRepository attributeRepo;
    private final CatalogAttributeOptionRepository optionRepo;
    private final CatalogCategoryAttributeRepository categoryAttributeRepo;
    private final CatalogConfigurationMapper configurationMapper;

    @Override
    @Transactional
    public CatalogCategoryResult createCategory(
            CreateCatalogCategoryPayload payload) {
        String slug = normalizeIdentifier(payload.getSlug());
        requireUniqueSlug(slug, null, payload.getSlug());

        CatalogCategoryEntity category = new CatalogCategoryEntity();
        category.setParent(requireValidParent(null, payload.getParentId()));
        applyMutableFields(category, payload, slug);
        category.setStatus(RecordStatus.ACTIVE);
        return configurationMapper.toCategoryResult(categoryRepo.save(category));
    }

    @Override
    @Transactional
    public CatalogCategoryResult updateCategory(
            UUID categoryId,
            UpdateCatalogCategoryPayload payload) {
        CatalogCategoryEntity category = requireCategoryForUpdate(categoryId);
        String slug = normalizeIdentifier(payload.getSlug());
        requireUniqueSlug(slug, categoryId, payload.getSlug());

        category.setParent(requireValidParent(
                categoryId,
                payload.getParentId()));
        applyMutableFields(category, payload, slug);
        return configurationMapper.toCategoryResult(category);
    }

    @Override
    @Transactional
    public void updateCategoryStatus(
            UUID categoryId,
            RecordStatus status) {
        requireCategoryForUpdate(categoryId).setStatus(status);
    }

    @Override
    public CatalogCategoryResult getCategory(UUID categoryId) {
        return configurationMapper.toCategoryResult(requireCategory(categoryId));
    }

    @Override
    public Page<CatalogCategoryResult> getCategories(
            CatalogCategoryFilterCriteria criteria,
            Pageable pageable) {
        return categoryRepo.findAll(
                CatalogCategorySpecification.withFilter(criteria),
                pageable)
                .map(configurationMapper::toCategoryResult);
    }

    @Override
    @Transactional
    public CatalogCategoryFormSchemaResult replaceCategoryAttributes(
            UUID categoryId,
            ReplaceCatalogCategoryAttributesPayload payload) {
        CatalogCategoryEntity category = requireCategoryForUpdate(categoryId);
        List<CatalogCategoryAttributePayload> requested =
                payload.getAttributes();
        requireDistinctAssignments(categoryId, requested);

        Map<UUID, CatalogAttributeEntity> attributesById = loadAttributes(
                requested.stream()
                        .map(CatalogCategoryAttributePayload::getAttributeId)
                        .toList());
        List<CatalogCategoryAttributeEntity> replacements = requested.stream()
                .map(item -> toMapping(category, item, attributesById))
                .toList();

        List<CatalogCategoryAttributeEntity> current = categoryAttributeRepo
                .findAllByCategory_IdOrderBySortOrderAsc(categoryId);
        categoryAttributeRepo.deleteAll(current);
        categoryAttributeRepo.flush();
        categoryAttributeRepo.saveAll(replacements);
        return buildConfigurationSchema(category);
    }

    @Override
    @Transactional
    public CatalogCategoryFormSchemaResult updateCategoryAttributeSelection(
            UUID categoryId,
            List<UUID> managedAttributeIds,
            ReplaceCatalogCategoryAttributesPayload payload) {
        CatalogCategoryEntity category = requireCategoryForUpdate(categoryId);
        Set<UUID> managedIds = new HashSet<>(managedAttributeIds);
        if (managedIds.size() != managedAttributeIds.size()) {
            throw ExceptionFactory.invalidParam(
                    "error.catalogCategory.attributeDuplicate",
                    categoryId);
        }

        List<CatalogCategoryAttributePayload> requested =
                payload.getAttributes();
        requireDistinctAssignments(categoryId, requested);
        Set<UUID> requestedIds = requested.stream()
                .map(CatalogCategoryAttributePayload::getAttributeId)
                .collect(Collectors.toSet());
        if (!managedIds.containsAll(requestedIds)) {
            throw ExceptionFactory.invalidParam(
                    "error.catalogCategory.attributeSelectionInvalid",
                    categoryId);
        }

        Map<UUID, CatalogAttributeEntity> attributesById = loadAttributes(
                managedIds);
        List<CatalogCategoryAttributeEntity> current = categoryAttributeRepo
                .findAllByCategory_IdOrderBySortOrderAsc(categoryId);
        Map<UUID, CatalogCategoryAttributeEntity> currentByAttributeId = current
                .stream()
                .collect(Collectors.toMap(
                        item -> item.getAttribute().getId(),
                        Function.identity()));
        requireAvailableSortOrders(
                categoryId,
                current,
                managedIds,
                requested);

        List<CatalogCategoryAttributeEntity> removals = current.stream()
                .filter(item -> managedIds.contains(
                        item.getAttribute().getId()))
                .filter(item -> !requestedIds.contains(
                        item.getAttribute().getId()))
                .toList();
        List<CatalogCategoryAttributeEntity> updates = requested.stream()
                .map(item -> upsertMapping(
                        category,
                        item,
                        attributesById,
                        currentByAttributeId))
                .toList();

        categoryAttributeRepo.deleteAll(removals);
        categoryAttributeRepo.saveAll(updates);
        return buildConfigurationSchema(category);
    }

    @Override
    public CatalogCategoryFormSchemaResult getCategoryConfiguration(
            UUID categoryId) {
        return buildConfigurationSchema(requireCategory(categoryId));
    }

    @Override
    public CatalogCategoryFormSchemaResult getActiveFormSchema(
            UUID categoryId) {
        return buildActiveFormSchema(requireActiveCategory(categoryId));
    }

    @Override
    public CatalogCategoryEntity requireCategory(UUID categoryId) {
        return categoryRepo.findDetailById(categoryId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.catalogCategory.notFound",
                        categoryId));
    }

    @Override
    public CatalogCategoryEntity requireActiveCategory(UUID categoryId) {
        CatalogCategoryEntity category = requireCategory(categoryId);
        if (category.getStatus() != RecordStatus.ACTIVE) {
            throw ExceptionFactory.invalidParam(
                    "error.catalogCategory.inactive",
                    categoryId);
        }
        return category;
    }

    private CatalogCategoryEntity requireCategoryForUpdate(UUID categoryId) {
        return categoryRepo.findForUpdateById(categoryId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.catalogCategory.notFound",
                        categoryId));
    }

    private CatalogCategoryEntity requireValidParent(
            UUID categoryId,
            UUID parentId) {
        if (parentId == null) {
            return null;
        }
        if (parentId.equals(categoryId)) {
            throw ExceptionFactory.invalidParam(
                    "error.catalogCategory.cycle",
                    parentId);
        }
        CatalogCategoryEntity parent = requireCategory(parentId);
        requireAcyclicParentChain(categoryId, parentId, parent);
        return parent;
    }

    private void requireAcyclicParentChain(
            UUID categoryId,
            UUID requestedParentId,
            CatalogCategoryEntity parent) {
        Set<UUID> visited = new HashSet<>();
        CatalogCategoryEntity current = parent;
        while (current != null) {
            if (!visited.add(current.getId())
                    || current.getId().equals(categoryId)) {
                throw ExceptionFactory.invalidParam(
                        "error.catalogCategory.cycle",
                        requestedParentId);
            }
            current = current.getParent();
        }
    }

    private void requireUniqueSlug(
            String slug,
            UUID categoryId,
            String rejectedValue) {
        boolean exists = categoryId == null
                ? categoryRepo.existsBySlug(slug)
                : categoryRepo.existsBySlugAndIdNot(slug, categoryId);
        if (exists) {
            throw ExceptionFactory.alreadyExists(
                    "slug",
                    rejectedValue,
                    "error.catalogCategory.slugAlreadyExists",
                    slug);
        }
    }

    private void requireDistinctAssignments(
            UUID categoryId,
            List<CatalogCategoryAttributePayload> requested) {
        Set<UUID> attributeIds = new HashSet<>();
        Set<Integer> positions = new HashSet<>();
        for (CatalogCategoryAttributePayload item : requested) {
            if (!attributeIds.add(item.getAttributeId())) {
                throw ExceptionFactory.invalidParam(
                        "error.catalogCategory.attributeDuplicate",
                        categoryId);
            }
            if (!positions.add(item.getSortOrder())) {
                throw ExceptionFactory.invalidParam(
                        "error.catalogCategory.sortOrderDuplicate",
                        categoryId);
            }
        }
    }

    private Map<UUID, CatalogAttributeEntity> loadAttributes(
            Collection<UUID> attributeIds) {
        if (attributeIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, CatalogAttributeEntity> attributesById = attributeRepo
                .findAllById(attributeIds)
                .stream()
                .collect(Collectors.toMap(
                        CatalogAttributeEntity::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        if (attributesById.size() != attributeIds.size()) {
            throw ExceptionFactory.notFound(
                    "error.catalogAttribute.notFound");
        }
        return attributesById;
    }

    private CatalogCategoryAttributeEntity toMapping(
            CatalogCategoryEntity category,
            CatalogCategoryAttributePayload payload,
            Map<UUID, CatalogAttributeEntity> attributesById) {
        CatalogAttributeEntity attribute = attributesById.get(
                payload.getAttributeId());
        requireActiveAttribute(attribute);
        CatalogCategoryAttributeEntity mapping =
                new CatalogCategoryAttributeEntity();
        mapping.setCategory(category);
        mapping.setAttribute(attribute);
        applyMappingFields(mapping, payload, attribute);
        return mapping;
    }

    private CatalogCategoryAttributeEntity upsertMapping(
            CatalogCategoryEntity category,
            CatalogCategoryAttributePayload payload,
            Map<UUID, CatalogAttributeEntity> attributesById,
            Map<UUID, CatalogCategoryAttributeEntity> currentByAttributeId) {
        CatalogAttributeEntity attribute = attributesById.get(
                payload.getAttributeId());
        CatalogCategoryAttributeEntity mapping = currentByAttributeId.get(
                attribute.getId());
        if (mapping == null) {
            requireActiveAttribute(attribute);
            mapping = new CatalogCategoryAttributeEntity();
            mapping.setCategory(category);
            mapping.setAttribute(attribute);
        }
        applyMappingFields(mapping, payload, attribute);
        return mapping;
    }

    private void applyMappingFields(
            CatalogCategoryAttributeEntity mapping,
            CatalogCategoryAttributePayload payload,
            CatalogAttributeEntity attribute) {
        if (payload.isCustomOptionAllowed()
                && attribute.getValueType()
                        != CatalogAttributeValueType.OPTION) {
            throw ExceptionFactory.invalidParam(
                    "error.catalogCategory.customOptionInvalid",
                    attribute.getId());
        }
        mapping.setRequired(payload.isRequired());
        mapping.setFilterable(payload.isFilterable());
        mapping.setMultipleValuesAllowed(
                payload.isMultipleValuesAllowed());
        mapping.setCustomOptionAllowed(payload.isCustomOptionAllowed());
        mapping.setSortOrder(payload.getSortOrder());
    }

    private void requireActiveAttribute(CatalogAttributeEntity attribute) {
        if (attribute.getStatus() != RecordStatus.ACTIVE) {
            throw ExceptionFactory.invalidParam(
                    "error.catalogAttribute.inactive",
                    attribute.getId());
        }
    }

    private void requireAvailableSortOrders(
            UUID categoryId,
            List<CatalogCategoryAttributeEntity> current,
            Set<UUID> managedAttributeIds,
            List<CatalogCategoryAttributePayload> requested) {
        Set<Integer> occupied = current.stream()
                .filter(item -> !managedAttributeIds.contains(
                        item.getAttribute().getId()))
                .map(CatalogCategoryAttributeEntity::getSortOrder)
                .collect(Collectors.toSet());
        for (CatalogCategoryAttributePayload item : requested) {
            if (!occupied.add(item.getSortOrder())) {
                throw ExceptionFactory.invalidParam(
                        "error.catalogCategory.sortOrderDuplicate",
                        categoryId);
            }
        }
    }

    private CatalogCategoryFormSchemaResult buildConfigurationSchema(
            CatalogCategoryEntity category) {
        List<CatalogCategoryAttributeEntity> mappings = categoryAttributeRepo
                .findAllByCategory_IdOrderBySortOrderAsc(category.getId());
        return toSchema(
                category,
                mappings,
                loadOptions(attributeIds(mappings)));
    }

    private CatalogCategoryFormSchemaResult buildActiveFormSchema(
            CatalogCategoryEntity category) {
        List<CatalogCategoryAttributeEntity> mappings = categoryAttributeRepo
                .findAllByCategory_IdAndAttribute_StatusOrderBySortOrderAsc(
                        category.getId(),
                        RecordStatus.ACTIVE);
        return toSchema(
                category,
                mappings,
                loadActiveOptions(attributeIds(mappings)));
    }

    private List<UUID> attributeIds(
            List<CatalogCategoryAttributeEntity> mappings) {
        return mappings.stream()
                .map(mapping -> mapping.getAttribute().getId())
                .toList();
    }

    private CatalogCategoryFormSchemaResult toSchema(
            CatalogCategoryEntity category,
            List<CatalogCategoryAttributeEntity> mappings,
            Map<UUID, List<CatalogAttributeOptionResult>> optionsByAttribute) {
        List<CatalogCategoryAttributeResult> attributeResults = mappings.stream()
                .map(mapping -> {
                    CatalogAttributeEntity attribute = mapping.getAttribute();
                    CatalogAttributeDefinitionResult attributeResult =
                            configurationMapper.toAttributeResult(
                                    attribute,
                                    optionsByAttribute.getOrDefault(
                                            attribute.getId(),
                                            List.of()));
                    return configurationMapper.toCategoryAttributeResult(
                            mapping,
                            attributeResult);
                })
                .toList();

        CatalogCategoryFormSchemaResult result =
                new CatalogCategoryFormSchemaResult();
        result.setCategory(configurationMapper.toCategoryResult(category));
        result.setAttributes(attributeResults);
        return result;
    }

    private Map<UUID, List<CatalogAttributeOptionResult>> loadOptions(
            List<UUID> attributeIds) {
        if (attributeIds.isEmpty()) {
            return Map.of();
        }
        return groupOptions(optionRepo
                .findAllByAttribute_IdInOrderByAttribute_IdAscSortOrderAsc(
                        attributeIds));
    }

    private Map<UUID, List<CatalogAttributeOptionResult>> loadActiveOptions(
            List<UUID> attributeIds) {
        if (attributeIds.isEmpty()) {
            return Map.of();
        }
        return groupOptions(optionRepo
                .findAllByAttribute_IdInAndStatusOrderByAttribute_IdAscSortOrderAsc(
                        attributeIds,
                        RecordStatus.ACTIVE));
    }

    private Map<UUID, List<CatalogAttributeOptionResult>> groupOptions(
            List<CatalogAttributeOptionEntity> options) {
        return options.stream()
                .collect(Collectors.groupingBy(
                        option -> option.getAttribute().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                configurationMapper::toOptionResult,
                                Collectors.toList())));
    }

    private void applyMutableFields(
            CatalogCategoryEntity category,
            CreateCatalogCategoryPayload payload,
            String slug) {
        category.setName(payload.getName().trim());
        category.setSlug(slug);
        category.setDescription(normalizeOptional(payload.getDescription()));
        category.setSortOrder(payload.getSortOrder());
    }

}
