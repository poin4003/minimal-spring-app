package com.app.features.notification.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.notification.entity.NotificationEntity;
import com.app.features.notification.entity.NotificationEntity_;
import com.app.features.notification.repository.NotificationRepository;
import com.app.features.notification.service.NotificationPolicyService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class NotificationPolicyServiceImpl
        implements NotificationPolicyService {

    private final NotificationRepository notificationRepo;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public long enforceHardLimit(UUID recipientId) {
        int hardLimit = appProperties.getNotification()
                .getPolicy()
                .getHardLimitPerUser();
        Pageable overflowPage = PageRequest.of(
                1,
                hardLimit,
                Sort.by(
                        Sort.Order.desc(NotificationEntity_.CREATED_AT),
                        Sort.Order.desc(NotificationEntity_.ID)));

        long deletedCount = 0;
        List<NotificationEntity> overflow;

        do {
            overflow = notificationRepo.findAllByRecipient_Id(
                    recipientId,
                    overflowPage);

            if (!overflow.isEmpty()) {
                notificationRepo.deleteAllInBatch(overflow);
                deletedCount += overflow.size();
            }
        } while (!overflow.isEmpty());

        return deletedCount;
    }

    @Override
    @Transactional
    public long deleteExpiredNotifications() {
        Duration ttl = appProperties.getNotification()
                .getPolicy()
                .getTtl();
        LocalDateTime cutoff = LocalDateTime.now().minus(ttl);

        return notificationRepo.deleteExpiredNotifications(cutoff);
    }

    @Override
    @Transactional
    public long deleteOverflowNotifications() {
        int hardLimit = appProperties.getNotification()
                .getPolicy()
                .getHardLimitPerUser();

        return notificationRepo.deleteOverflowNotifications(hardLimit);
    }
}
