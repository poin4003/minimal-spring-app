package com.app.features.notification.service;

import com.app.features.notification.enums.NotificationChannel;
import com.app.features.notification.schema.model.NotificationDeliveryMessage;

public interface NotificationChannelSender {

    NotificationChannel getChannel();

    String send(NotificationDeliveryMessage message);
}
