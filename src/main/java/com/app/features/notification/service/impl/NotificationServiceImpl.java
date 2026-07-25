package com.app.features.notification.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.notification.entity.NotificationEntity;
import com.app.features.notification.repository.NotificationRepository;
import com.app.features.notification.repository.spec.NotificationSpecification;
import com.app.features.notification.schema.filter.NotificationFilterCriteria;
import com.app.features.notification.schema.payload.CreateNotificationPayload;
import com.app.features.notification.schema.result.NotificationResult;
import com.app.features.notification.service.NotificationService;
import com.app.features.user.repository.UserBaseRepository;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserBaseRepository userBaseRepo;
    private final ModelMapper mapper;

    @Override
    @Transactional
    public NotificationResult createNotification(CreateNotificationPayload payload) {
        return saveNotification(payload);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResult createNotificationIfAbsent(
            CreateNotificationPayload payload) {
        return notificationRepo
                .findByRecipient_IdAndTypeAndResourceTypeAndResourceId(
                        payload.getRecipientId(),
                        payload.getType(),
                        payload.getResourceType(),
                        payload.getResourceId())
                .map(notification -> mapper.map(notification, NotificationResult.class))
                .orElseGet(() -> saveNotification(payload));
    }

    private NotificationResult saveNotification(
            CreateNotificationPayload payload) {
        NotificationEntity notification = new NotificationEntity();
        notification.setRecipient(
                userBaseRepo.getReferenceById(payload.getRecipientId()));
        notification.setActor(payload.getActorId() == null
                ? null
                : userBaseRepo.getReferenceById(payload.getActorId()));
        notification.setType(payload.getType());
        notification.setResourceType(payload.getResourceType());
        notification.setResourceId(payload.getResourceId());
        notification.setTitle(payload.getTitle().trim());
        notification.setContent(payload.getContent().trim());

        notification = notificationRepo.save(notification);
        return mapper.map(notification, NotificationResult.class);
    }

    @Override
    public Page<NotificationResult> getManyNotifications(
            NotificationFilterCriteria criteria,
            Pageable pageable) {
        Specification<NotificationEntity> spec =
                NotificationSpecification.withFilter(criteria);

        return notificationRepo.findAll(spec, pageable)
                .map(notification -> mapper.map(notification, NotificationResult.class));
    }

    @Override
    public NotificationResult getNotification(
            UUID recipientId,
            UUID notificationId) {
        NotificationEntity notification = requireOwned(recipientId, notificationId);
        return mapper.map(notification, NotificationResult.class);
    }

    @Override
    @Transactional
    public NotificationResult markAsRead(
            UUID recipientId,
            UUID notificationId) {
        NotificationEntity notification = requireOwned(recipientId, notificationId);

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }

        return mapper.map(notification, NotificationResult.class);
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID recipientId) {
        return notificationRepo.markAllAsRead(recipientId, LocalDateTime.now());
    }

    private NotificationEntity requireOwned(
            UUID recipientId,
            UUID notificationId) {
        return notificationRepo.findByIdAndRecipient_Id(notificationId, recipientId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "Notification: " + notificationId));
    }
}
