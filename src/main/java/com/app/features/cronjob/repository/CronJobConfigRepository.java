package com.app.features.cronjob.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.cronjob.entity.CronJobConfigEntity;

public interface CronJobConfigRepository extends JpaRepository<CronJobConfigEntity, UUID> {

    Optional<CronJobConfigEntity> findByJobType(String jobType);
}
