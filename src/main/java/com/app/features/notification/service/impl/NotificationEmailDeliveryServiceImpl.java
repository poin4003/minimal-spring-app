package com.app.features.notification.service.impl;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.email.enums.EmailTemplate;
import com.app.features.email.service.EmailTemplateService;
import com.app.features.notification.entity.NotificationEntity;
import com.app.features.notification.enums.NotificationChannel;
import com.app.features.notification.repository.NotificationRepository;
import com.app.features.notification.repository.UserNotificationPreferenceRepository;
import com.app.features.notification.schema.model.NotificationEmailTemplateModel;
import com.app.features.notification.schema.payload.CreateNotificationDeliveryPayload;
import com.app.features.notification.service.NotificationDeliveryService;
import com.app.features.notification.service.NotificationEmailDeliveryService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class NotificationEmailDeliveryServiceImpl
        implements NotificationEmailDeliveryService {

    private final UserNotificationPreferenceRepository
            userNotificationPreferenceRepo;
    private final NotificationRepository notificationRepo;
    private final NotificationDeliveryService notificationDeliverySvc;
    private final EmailTemplateService emailTemplateSvc;
    private final ModelMapper mapper;

    @Override
    public void createDeliveryIfEnabled(
            UUID notificationId,
            UUID recipientId,
            EmailTemplate template) {
        userNotificationPreferenceRepo
                .findByIdAndEmailEnabledTrue(recipientId)
                .ifPresent(preference -> {
                    NotificationEntity notification = notificationRepo
                            .findByIdAndRecipient_Id(
                                    notificationId,
                                    recipientId)
                            .orElseThrow(() -> ExceptionFactory.notFound(
                                    "Notification: " + notificationId));
                    NotificationEmailTemplateModel model = mapper.map(
                            notification,
                            NotificationEmailTemplateModel.class);
                    String html = emailTemplateSvc.render(template, model);

                    CreateNotificationDeliveryPayload payload =
                            new CreateNotificationDeliveryPayload();
                    payload.setNotificationId(notificationId);
                    payload.setChannel(NotificationChannel.EMAIL);
                    payload.setRecipientAddress(
                            preference.getUser().getEmail());
                    payload.setSubjectSnapshot(notification.getTitle());
                    payload.setContentSnapshot(html);

                    notificationDeliverySvc
                            .createDeliveryIfAbsent(payload);
                });
    }
}
