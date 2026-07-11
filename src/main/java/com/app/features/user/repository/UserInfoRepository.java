package com.app.features.user.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.user.entity.UserInfoEntity;

public interface UserInfoRepository extends JpaRepository<UserInfoEntity, UUID> {

}
