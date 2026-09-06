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
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(
        name = "catalog_offer",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_catalog_offer_position",
                        columnNames = {"catalog_post_id", "position"})
        },
        indexes = {
                @Index(
                        name = "idx_catalog_offer_post_status_position",
                        columnList = "catalog_post_id, status, position"),
                @Index(
                        name = "idx_catalog_offer_post_default_status",
                        columnList = "catalog_post_id, is_default, status")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class CatalogOfferEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_post_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CatalogPostEntity catalogPost;

    @Size(max = 255)
    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean defaultOffer;

    @PositiveOrZero
    @Column(name = "position", nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private RecordStatus status = RecordStatus.ACTIVE;
}
