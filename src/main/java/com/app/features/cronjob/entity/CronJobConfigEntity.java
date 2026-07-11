package com.app.features.cronjob.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.features.cronjob.enums.CronjobStatusEnum;

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
@Table(name = "cronjob_config")
@Data
@EqualsAndHashCode(callSuper = true)
public class CronJobConfigEntity extends BaseAuditEntity {

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CronjobStatusEnum status;
}
