package com.app.features.cronjob.entity;

import java.util.UUID;

import com.app.core.base.BaseEntity;
import com.app.features.cronjob.enums.CronjobStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "cronjob_config")
@Data
@EqualsAndHashCode(callSuper = true)
public class CronJobConfigEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "expression")
    private String expression;

    @Column(name = "job_type")
    private String jobType;

    @Column(name = "lock_at_most_for")
    private String lockAtMostFor;

    @Column(name = "lock_at_least_for")
    private String lockAtLeastFor;

    @Column(name = "status", nullable = false)
    private CronjobStatusEnum status;
}
