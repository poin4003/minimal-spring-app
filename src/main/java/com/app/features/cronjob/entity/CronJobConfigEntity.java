package com.app.features.cronjob.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.core.enums.RecordStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "cronjob_config", indexes = {
        @Index(name = "idx_cronjob_config_created_at", columnList = "created_at"),
        @Index(name = "idx_cronjob_config_updated_at", columnList = "updated_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class CronJobConfigEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_type", nullable = false, updatable = false)
    private String jobType;

    @Column(name = "expression", length = 100)
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) 
    @Column(nullable = false)
    private RecordStatus status;
}
