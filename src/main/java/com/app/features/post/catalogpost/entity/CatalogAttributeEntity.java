package com.app.features.post.catalogpost.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.core.enums.RecordStatus;
import com.app.features.post.catalogpost.enums.CatalogAttributeValueType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "catalog_attribute",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_catalog_attribute_key",
                        columnNames = "attribute_key")
        },
        indexes = {
                @Index(
                        name = "idx_catalog_attribute_status_name",
                        columnList = "status, name")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class CatalogAttributeEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(
            name = "attribute_key",
            nullable = false,
            length = 100,
            updatable = false)
    private String key;

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "value_type", nullable = false)
    private CatalogAttributeValueType valueType;

    @Size(max = 32)
    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "searchable", nullable = false)
    private boolean searchable = true;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private RecordStatus status = RecordStatus.ACTIVE;
}
