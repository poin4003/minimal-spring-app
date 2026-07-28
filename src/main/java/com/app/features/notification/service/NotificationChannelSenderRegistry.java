package com.app.features.notification.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.app.core.exception.ExceptionFactory;
import com.app.features.notification.enums.NotificationChannel;

@Component
public class NotificationChannelSenderRegistry {

    private final Map<NotificationChannel, NotificationChannelSender> senders;

    public NotificationChannelSenderRegistry(
            List<NotificationChannelSender> senderList) {
        EnumMap<NotificationChannel, NotificationChannelSender> senderMap =
                new EnumMap<>(NotificationChannel.class);

        for (NotificationChannelSender sender : senderList) {
            NotificationChannelSender previous =
                    senderMap.put(sender.getChannel(), sender);

            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate notification sender: "
                                + sender.getChannel());
            }
        }

        senders = Map.copyOf(senderMap);
    }

    public NotificationChannelSender require(
            NotificationChannel channel) {
        NotificationChannelSender sender = senders.get(channel);

        if (sender == null) {
            throw ExceptionFactory.serverError(
                    "error.notification.senderNotConfigured",
                    channel);
        }

        return sender;
    }
}
