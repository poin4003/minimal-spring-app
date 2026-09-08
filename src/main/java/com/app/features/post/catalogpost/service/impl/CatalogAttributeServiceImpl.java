package com.app.features.post.catalogpost.service.impl;

import static com.app.core.utils.TextNormalizationUtils.normalizeIdentifier;
import static com.app.core.utils.TextNormalizationUtils.normalizeOptional;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.enums.RecordStatus;
import com.app.core.exception.ExceptionFactory;
import com.app.features.post.catalogpost.entity.CatalogAttributeEntity;
import com.app.features.post.catalogpost.entity.CatalogAttributeOptionEntity;
import com.app.features.post.catalogpost.enums.CatalogAttributeValueType;
import com.app.features.post.catalogpost.mapper.CatalogConfigurationMapper;
import com.app.features.post.catalogpost.repository.CatalogAttributeOptionRepository;
import com.app.features.post.catalogpost.repository.CatalogAttributeRepository;
import com.app.features.post.catalogpost.repository.CatalogOfferAttributeValueRepository;
import com.app.features.post.catalogpost.repository.CatalogPostAttributeValueRepository;
import com.app.features.post.catalogpost.repository.spec.CatalogAttributeSpecification;
import com.app.features.post.catalogpost.schema.filter.CatalogAttributeFilterCriteria;
import com.app.features.post.catalogpost.schema.payload.CreateCatalogAttributeOptionPayload;
import com.app.features.post.catalogpost.schema.payload.CreateCatalogAttributePayload;
import com.app.features.post.catalogpost.schema.payload.UpdateCatalogAttributeOptionPayload;
import com.app.features.post.catalogpost.schema.payload.UpdateCatalogAttributePayload;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeDefinitionResult;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeOptionResult;
import com.app.features.post.catalogpost.service.CatalogAttributeService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogAttributeServiceImpl implements CatalogAttributeService {

    private final CatalogAttributeRepository attributeRepo;
    private final CatalogAttributeOptionRepository optionRepo;
    private final CatalogPostAttributeValueRepository postValueRepo;
    private final CatalogOfferAttributeValueRepository offerValueRepo;
    private final CatalogConfigurationMapper configurationMapper;

    @Override
    @Transactional
    public CatalogAttributeDefinitionResult createAttribute(
            CreateCatalogAttributePayload payload) {
        String key = normalizeIdentifier(payload.getKey());
        if (attributeRepo.existsByKey(key)) {
            throw ExceptionFactory.alreadyExists(
                    "key",
                    payload.getKey(),
                    "error.catalogAttribute.keyAlreadyExists",
                    key);
        }

        CatalogAttributeEntity attribute = new CatalogAttributeEntity();
        attribute.setKey(key);
        applyMutableFields(attribute, payload);
        attribute.setStatus(RecordStatus.ACTIVE);
        return configurationMapper.toAttributeResult(
                attributeRepo.save(attribute));
    }

    @Override
    @Transactional
    public CatalogAttributeDefinitionResult updateAttribute(
            UUID attributeId,
            UpdateCatalogAttributePayload payload) {
        CatalogAttributeEntity attribute = requireAttributeForUpdate(
                attributeId);
        requireSafeValueTypeChange(attribute, payload.getValueType());

        attribute.setName(payload.getName().trim());
        attribute.setDescription(normalizeOptional(payload.getDescription()));
        attribute.setValueType(payload.getValueType());
        attribute.setUnit(normalizeOptional(payload.getUnit()));
        attribute.setSearchable(payload.isSearchable());
        return configurationMapper.toAttributeResult(attribute);
    }

    @Override
    @Transactional
    public void updateAttributeStatus(
            UUID attributeId,
            RecordStatus status) {
        requireAttributeForUpdate(attributeId).setStatus(status);
    }

    @Override
    public CatalogAttributeDefinitionResult getAttribute(UUID attributeId) {
        return configurationMapper.toAttributeResult(
                requireAttribute(attributeId));
    }

    @Override
    public Page<CatalogAttributeDefinitionResult> getAttributes(
            CatalogAttributeFilterCriteria criteria,
            Pageable pageable) {
        return attributeRepo.findAll(
                CatalogAttributeSpecification.withFilter(criteria),
                pageable)
                .map(configurationMapper::toAttributeResult);
    }

    @Override
    @Transactional
    public CatalogAttributeOptionResult createOption(
            UUID attributeId,
            CreateCatalogAttributeOptionPayload payload) {
        CatalogAttributeEntity attribute = requireOptionAttribute(attributeId);
        String code = normalizeIdentifier(payload.getCode());
        if (optionRepo.existsByAttribute_IdAndCode(attributeId, code)) {
            throw ExceptionFactory.alreadyExists(
                    "code",
                    payload.getCode(),
                    "error.catalogAttributeOption.codeAlreadyExists",
                    code);
        }

        CatalogAttributeOptionEntity option =
                new CatalogAttributeOptionEntity();
        option.setAttribute(attribute);
        option.setCode(code);
        option.setLabel(payload.getLabel().trim());
        option.setSortOrder(payload.getSortOrder());
        option.setStatus(RecordStatus.ACTIVE);
        return configurationMapper.toOptionResult(optionRepo.save(option));
    }

    @Override
    @Transactional
    public CatalogAttributeOptionResult updateOption(
            UUID attributeId,
            UUID optionId,
            UpdateCatalogAttributeOptionPayload payload) {
        CatalogAttributeOptionEntity option = requireOwnedOptionForUpdate(
                attributeId,
                optionId);
        requireOptionAttribute(attributeId);
        option.setLabel(payload.getLabel().trim());
        option.setSortOrder(payload.getSortOrder());
        return configurationMapper.toOptionResult(option);
    }

    @Override
    @Transactional
    public void updateOptionStatus(
            UUID attributeId,
            UUID optionId,
            RecordStatus status) {
        requireOwnedOptionForUpdate(attributeId, optionId).setStatus(status);
    }

    @Override
    public Page<CatalogAttributeOptionResult> getOptions(
            UUID attributeId,
            Pageable pageable) {
        requireAttribute(attributeId);
        return optionRepo.findAllByAttribute_Id(attributeId, pageable)
                .map(configurationMapper::toOptionResult);
    }

    @Override
    public CatalogAttributeOptionResult getOption(
            UUID attributeId,
            UUID optionId) {
        return optionRepo.findByIdAndAttribute_Id(optionId, attributeId)
                .map(configurationMapper::toOptionResult)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.catalogAttributeOption.notFound",
                        optionId));
    }

    @Override
    public CatalogAttributeEntity requireAttribute(UUID attributeId) {
        return attributeRepo.findById(attributeId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.catalogAttribute.notFound",
                        attributeId));
    }

    @Override
    public CatalogAttributeEntity requireActiveAttribute(UUID attributeId) {
        CatalogAttributeEntity attribute = requireAttribute(attributeId);
        if (attribute.getStatus() != RecordStatus.ACTIVE) {
            throw ExceptionFactory.invalidParam(
                    "error.catalogAttribute.inactive",
                    attributeId);
        }
        return attribute;
    }

    private CatalogAttributeEntity requireAttributeForUpdate(UUID attributeId) {
        return attributeRepo.findForUpdateById(attributeId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.catalogAttribute.notFound",
                        attributeId));
    }

    private CatalogAttributeEntity requireOptionAttribute(UUID attributeId) {
        CatalogAttributeEntity attribute = requireAttribute(attributeId);
        if (attribute.getValueType() != CatalogAttributeValueType.OPTION) {
            throw ExceptionFactory.invalidParam(
                    "error.catalogAttribute.optionTypeRequired",
                    attributeId);
        }
        return attribute;
    }

    private CatalogAttributeOptionEntity requireOwnedOptionForUpdate(
            UUID attributeId,
            UUID optionId) {
        CatalogAttributeOptionEntity option = optionRepo
                .findForUpdateById(optionId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.catalogAttributeOption.notFound",
                        optionId));
        if (!option.getAttribute().getId().equals(attributeId)) {
            throw ExceptionFactory.notFound(
                    "error.catalogAttributeOption.notFound",
                    optionId);
        }
        return option;
    }

    private void requireSafeValueTypeChange(
            CatalogAttributeEntity attribute,
            CatalogAttributeValueType valueType) {
        if (attribute.getValueType() == valueType) {
            return;
        }

        UUID attributeId = attribute.getId();
        if (optionRepo.existsByAttribute_Id(attributeId)
                || postValueRepo.existsByAttribute_Id(attributeId)
                || offerValueRepo.existsByAttribute_Id(attributeId)) {
            throw ExceptionFactory.invalidParam(
                    "error.catalogAttribute.valueTypeInUse",
                    attributeId);
        }
    }

    private void applyMutableFields(
            CatalogAttributeEntity attribute,
            CreateCatalogAttributePayload payload) {
        attribute.setName(payload.getName().trim());
        attribute.setDescription(normalizeOptional(payload.getDescription()));
        attribute.setValueType(payload.getValueType());
        attribute.setUnit(normalizeOptional(payload.getUnit()));
        attribute.setSearchable(payload.isSearchable());
    }

}
