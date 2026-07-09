package com.app.features.sims.entity;

import java.util.UUID;

import com.app.core.base.BaseEntity;
import com.app.features.sims.enums.SimStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class SimEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(name = "status", nullable = false)
    private SimStatusEnum status;

    @Column(name = "selling_price")
    private Integer sellingPrice;

    @Column(name = "dealer_price")
    private Integer dealerPrice;

    @Column(name = "import_price")
    private Integer importPrice;
}
