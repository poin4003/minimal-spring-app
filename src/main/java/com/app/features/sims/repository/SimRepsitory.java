package com.app.features.sims.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.app.features.sims.entity.SimEntity;

public interface SimRepsitory extends JpaRepository<SimEntity, UUID>, JpaSpecificationExecutor<SimEntity> {
    boolean existsByPhoneNumber(String phoneNumber);
}
