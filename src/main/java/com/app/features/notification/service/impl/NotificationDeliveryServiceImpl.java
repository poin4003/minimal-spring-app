package com.app.features.notification.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.notification.entity.NotificationDeliveryEntity;
import com.app.features.notification.entity.NotificationEntity;
import com.app.features.notification.enums.NotificationDeliveryStatus;
import com.app.features.notification.job.NotificationDeliveryJob;
import com.app.features.notification.repository.NotificationDeliveryRepository;
import com.app.features.notification.repository.NotificationRepository;
import com.app.features.notification.schema.model.NotificationDeliveryMessage;
import com.app.features.notification.schema.payload.CreateNotificationDeliveryPayload;
import com.app.features.notification.service.NotificationChannelSender;
import com.app.features.notification.service.NotificationChannelSenderRegistry;
import com.app.features.notification.service.NotificationDeliveryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class NotificationDeliveryServiceImpl
        implements NotificationDeliveryService {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final NotificationDeliveryRepository notificationDeliveryRepo;
    private final NotificationRepository notificationRepo;
    private final NotificationChannelSenderRegistry senderRegistry;
    private final JobScheduler jobScheduler;

    @Override
    @Transactional
    public UUID createDeliveryIfAbsent(
            CreateNotificationDeliveryPayload payload) {
        Optional<NotificationDeliveryEntity> existing =
                notificationDeliveryRepo
                        .findByNotification_IdAndChannel(
                                payload.getNotificationId(),
                                payload.getChannel());

        if (existing.isPresent()) {
            return existing.get().getId();
        }

        NotificationEntity notification = notificationRepo
                .findById(payload.getNotificationId())
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "Notification: " + payload.getNotificationId()));

        NotificationDeliveryEntity delivery =
                new NotificationDeliveryEntity();
        delivery.setNotification(notification);
        delivery.setChannel(payload.getChannel());
        delivery.setRecipientAddress(
                payload.getRecipientAddress().trim());
        delivery.setSubjectSnapshot(payload.getSubjectSnapshot());
        delivery.setContentSnapshot(payload.getContentSnapshot());
        delivery.setStatus(NotificationDeliveryStatus.PENDING);
        delivery.setAttemptCount(0);

        delivery = notificationDeliveryRepo.save(delivery);
        registerDeliveryJob(delivery.getId());

        return delivery.getId();
    }

    @Override
    public void processDelivery(UUID deliveryId) {
        NotificationDeliveryMessage message =
                prepareDelivery(deliveryId);

        if (message == null) {
            return;
        }

        try {
            NotificationChannelSender sender =
                    senderRegistry.require(message.getChannel());
            String providerMessageId = sender.send(message);

            markSent(deliveryId, providerMessageId);
        } catch (RuntimeException exception) {
            markFailed(deliveryId, exception);
            throw exception;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected NotificationDeliveryMessage prepareDelivery(
            UUID deliveryId) {
        NotificationDeliveryEntity delivery =
                requireDeliveryForUpdate(deliveryId);

        if (delivery.getStatus() == NotificationDeliveryStatus.SENT) {
            return null;
        }

        delivery.setStatus(NotificationDeliveryStatus.PROCESSING);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastError(null);

        return NotificationDeliveryMessage.builder()
                .deliveryId(delivery.getId())
                .channel(delivery.getChannel())
                .recipientAddress(delivery.getRecipientAddress())
                .subject(delivery.getSubjectSnapshot())
                .content(delivery.getContentSnapshot())
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markSent(
            UUID deliveryId,
            String providerMessageId) {
        NotificationDeliveryEntity delivery =
                requireDeliveryForUpdate(deliveryId);

        delivery.setStatus(NotificationDeliveryStatus.SENT);
        delivery.setProviderMessageId(providerMessageId);
        delivery.setLastError(null);
        delivery.setSentAt(LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markFailed(
            UUID deliveryId,
            RuntimeException exception) {
        NotificationDeliveryEntity delivery =
                requireDeliveryForUpdate(deliveryId);

        String error = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();

        delivery.setStatus(NotificationDeliveryStatus.FAILED);
        delivery.setLastError(error.substring(
                0,
                Math.min(error.length(), MAX_ERROR_LENGTH)));
    }

    private NotificationDeliveryEntity requireDeliveryForUpdate(
            UUID deliveryId) {
        return notificationDeliveryRepo.findOneById(deliveryId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "Notification delivery: " + deliveryId));
    }

    private void registerDeliveryJob(UUID deliveryId) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            jobScheduler.<NotificationDeliveryJob>enqueue(
                                    job -> job.execute(
                                            deliveryId,
                                            JobContext.Null));
                        } catch (RuntimeException exception) {
                            log.error(
                                    "Unable to enqueue notification delivery [{}].",
                                    deliveryId,
                                    exception);
                        }
                    }
                });
    }
}
