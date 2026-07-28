package com.app.features.notification.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.app.core.enums.AppLanguage;
import com.app.core.i18n.AppMessageResolver;
import com.app.features.notification.schema.model.NotificationTextSnapshot;
import com.app.features.notification.schema.payload.NotificationTextPayload;
import com.app.features.user.repository.UserInfoRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationTextResolver {

    private final UserInfoRepository userInfoRepo;
    private final AppMessageResolver messageResolver;

    public NotificationTextSnapshot resolve(
            UUID recipientId,
            NotificationTextPayload payload) {
        AppLanguage language = userInfoRepo
                .findLanguageByUserId(recipientId)
                .orElse(AppLanguage.EN);

        return new NotificationTextSnapshot(
                messageResolver.get(
                        language.toLocale(),
                        payload.getTitleKey(),
                        payload.getTitleArguments().toArray()),
                messageResolver.get(
                        language.toLocale(),
                        payload.getContentKey(),
                        payload.getContentArguments().toArray()));
    }
}
