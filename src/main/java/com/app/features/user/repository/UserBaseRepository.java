package com.app.features.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.app.features.user.entity.UserBaseEntity;

@Repository
public interface UserBaseRepository extends JpaRepository<UserBaseEntity, UUID> {
    boolean existsByEmail(String email);

    @Query("""
                SELECT DISTINCT u FROM UserBaseEntity u
                LEFT JOIN FETCH u.roles r
                LEFT JOIN FETCH r.permissions
                WHERE u.email = :email
            """)
    Optional<UserBaseEntity> findByEmail(String email);
}
