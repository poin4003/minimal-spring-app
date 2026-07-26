package com.app.features.notification.job;

import java.util.UUID;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.springframework.stereotype.Component;

import com.app.features.notification.service.NotificationDeliveryService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryJob {

    private final NotificationDeliveryService notificationDeliverySvc;

    @Job(name = "Deliver external notification", retries = 3)
    public void execute(
            UUID deliveryId,
            JobContext jobContext) {
        notificationDeliverySvc.processDelivery(deliveryId);
    }
}
