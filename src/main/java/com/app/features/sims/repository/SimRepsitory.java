package com.app.features.sims.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.app.features.sims.entity.SimEntity;

@Repository
public interface SimRepsitory extends JpaRepository<SimEntity, UUID>, JpaSpecificationExecutor<SimEntity> {
    boolean existsByPhoneNumber(String phoneNumber);
}
