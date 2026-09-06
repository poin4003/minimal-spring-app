package com.app.features.post.catalogpost.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(
        name = "catalog_category_attribute",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_catalog_category_attribute",
                        columnNames = {"category_id", "attribute_id"})
        },
        indexes = {
                @Index(
                        name = "idx_catalog_cat_attr_category_filter_order",
                        columnList = "category_id, filterable, sort_order"),
                @Index(
                        name = "idx_catalog_cat_attr_attribute_category",
                        columnList = "attribute_id, category_id")
        })
@Data
public class CatalogCategoryAttributeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CatalogCategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CatalogAttributeEntity attribute;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "filterable", nullable = false)
    private boolean filterable;

    @Column(name = "multiple_values_allowed", nullable = false)
    private boolean multipleValuesAllowed;

    @Column(name = "custom_option_allowed", nullable = false)
    private boolean customOptionAllowed;

    @PositiveOrZero
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
