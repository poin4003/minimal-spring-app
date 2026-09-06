package com.app.features.post.catalogpost.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(
        name = "catalog_post_attribute_value",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_catalog_post_attr_value",
                        columnNames = {
                                "catalog_post_id", "attribute_id", "position"
                        }),
                @UniqueConstraint(
                        name = "uk_catalog_post_custom_value",
                        columnNames = {
                                "catalog_post_id", "custom_key", "position"
                        })
        },
        indexes = {
                @Index(
                        name = "idx_catalog_post_attr_value_order",
                        columnList = "catalog_post_id, position"),
                @Index(
                        name = "idx_catalog_post_attr_number",
                        columnList = "attribute_id, number_value, catalog_post_id"),
                @Index(
                        name = "idx_catalog_post_attr_option",
                        columnList = "attribute_id, option_id, catalog_post_id"),
                @Index(
                        name = "idx_catalog_post_attr_boolean",
                        columnList = "attribute_id, boolean_value, catalog_post_id"),
                @Index(
                        name = "idx_catalog_post_attr_date",
                        columnList = "attribute_id, date_value, catalog_post_id"),
                @Index(
                        name = "idx_catalog_post_attr_custom_key",
                        columnList = "custom_key, catalog_post_id")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class CatalogPostAttributeValueEntity
        extends AbstractCatalogAttributeValueEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_post_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CatalogPostEntity catalogPost;
}
