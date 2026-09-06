package com.app.features.post.catalogpost.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.features.post.catalogpost.enums.CatalogReferenceType;
import com.app.features.post.entity.PostEntity;

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
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(
        name = "post_catalog_reference",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_catalog_reference_target",
                        columnNames = {"post_id", "catalog_post_id"}),
                @UniqueConstraint(
                        name = "uk_post_catalog_reference_position",
                        columnNames = {"post_id", "position"})
        },
        indexes = {
                @Index(
                        name = "idx_post_catalog_ref_catalog_type_post",
                        columnList = "catalog_post_id, reference_type, post_id")
        })
@Data
public class PostCatalogReferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_post_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CatalogPostEntity catalogPost;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reference_type", nullable = false)
    private CatalogReferenceType referenceType;

    @PositiveOrZero
    @Column(name = "position", nullable = false)
    private int position;
}
