package com.app.features.cronjob.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.features.cronjob.entity.CronJobConfigEntity;
import com.app.features.cronjob.enums.CronjobStatusEnum;

@Repository
public interface CronJobConfigRepository extends JpaRepository<CronJobConfigEntity, UUID> {

    List<CronJobConfigEntity> findByStatus(CronjobStatusEnum status);
}
