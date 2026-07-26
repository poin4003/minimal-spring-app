package com.app.features.notification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.app.features.notification.entity.NotificationDeliveryEntity;
import com.app.features.notification.enums.NotificationChannel;

import jakarta.persistence.LockModeType;

public interface NotificationDeliveryRepository
        extends JpaRepository<NotificationDeliveryEntity, UUID> {

    Optional<NotificationDeliveryEntity> findByNotification_IdAndChannel(
            UUID notificationId,
            NotificationChannel channel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<NotificationDeliveryEntity> findOneById(UUID deliveryId);
}
