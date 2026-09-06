package com.app.features.post.catalogpost.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.core.enums.RecordStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(
        name = "catalog_attribute_option",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_catalog_attribute_option_code",
                        columnNames = {"attribute_id", "option_code"})
        },
        indexes = {
                @Index(
                        name = "idx_catalog_attr_option_status_order",
                        columnList = "attribute_id, status, sort_order")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class CatalogAttributeOptionEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CatalogAttributeEntity attribute;

    @NotBlank
    @Size(max = 100)
    @Column(
            name = "option_code",
            nullable = false,
            length = 100,
            updatable = false)
    private String code;

    @NotBlank
    @Size(max = 150)
    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @PositiveOrZero
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private RecordStatus status = RecordStatus.ACTIVE;
}
