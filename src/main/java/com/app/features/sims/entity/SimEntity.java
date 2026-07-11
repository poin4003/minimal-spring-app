package com.app.features.sims.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.features.sims.enums.SimStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "sim")
@Data
@EqualsAndHashCode(callSuper = true)
public class SimEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private SimStatusEnum status;

    @Column(name = "selling_price")
    private Integer sellingPrice;

    @Column(name = "dealer_price")
    private Integer dealerPrice;

    @Column(name = "import_price")
    private Integer importPrice;
}
