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
        name = "catalog_category",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_catalog_category_slug",
                        columnNames = "slug")
        },
        indexes = {
                @Index(
                        name = "idx_catalog_category_status_order",
                        columnList = "status, sort_order"),
                @Index(
                        name = "idx_catalog_category_parent_status_order",
                        columnList = "parent_id, status, sort_order")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class CatalogCategoryEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CatalogCategoryEntity parent;

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank
    @Size(max = 160)
    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @PositiveOrZero
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private RecordStatus status = RecordStatus.ACTIVE;
}
