package com.app.features.notification.service.impl;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.app.config.settings.AppProperties;
import com.app.features.notification.constant.NotificationSseEventNames;
import com.app.features.notification.enums.NotificationResourceType;
import com.app.features.notification.service.NotificationSseService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class NotificationSseServiceImpl implements NotificationSseService {

    private final ConcurrentMap<UUID, ConcurrentMap<UUID, SseEmitter>>
            connections = new ConcurrentHashMap<>();

    private final AppProperties appProperties;

    @Override
    public SseEmitter connect(UUID userId) {
        UUID connectionId = UUID.randomUUID();
        long timeout = appProperties.getNotification()
                .getSse()
                .getConnectionTimeout()
                .toMillis();
        SseEmitter emitter = new SseEmitter(timeout);

        connections.compute(userId, (ignored, userConnections) -> {
            ConcurrentMap<UUID, SseEmitter> effectiveConnections =
                    userConnections == null
                            ? new ConcurrentHashMap<>()
                            : userConnections;
            effectiveConnections.put(connectionId, emitter);
            return effectiveConnections;
        });

        emitter.onCompletion(() -> remove(userId, connectionId));
        emitter.onTimeout(() -> remove(userId, connectionId));
        emitter.onError(error -> remove(userId, connectionId));

        send(
                userId,
                connectionId,
                emitter,
                SseEmitter.event()
                        .name("connected")
                        .reconnectTime(3_000)
                        .data("connected"));

        return emitter;
    }

    @Override
    public void signalChanged(
            UUID userId,
            NotificationResourceType resourceType) {
        sendChangedEvent(
                userId,
                NotificationSseEventNames.NOTIFICATION);

        String resourceEventName =
                NotificationSseEventNames.BY_RESOURCE_TYPE.get(resourceType);
        if (resourceEventName != null) {
            sendChangedEvent(userId, resourceEventName);
        }
    }

    @Scheduled(
            fixedDelayString =
                    "${app.notification.sse.heartbeat-interval:20s}")
    public void heartbeat() {
        connections.keySet().forEach(userId ->
                sendToUser(
                        userId,
                        () -> SseEmitter.event().comment("heartbeat")));
    }

    @Override
    public boolean isOnline(UUID userId) {
        ConcurrentMap<UUID, SseEmitter> userConnections =
                connections.get(userId);
        return userConnections != null && !userConnections.isEmpty();
    }

    @Override
    public int countConnections(UUID userId) {
        ConcurrentMap<UUID, SseEmitter> userConnections =
                connections.get(userId);
        return userConnections == null ? 0 : userConnections.size();
    }

    @Override
    public Set<UUID> getOnlineUserIds() {
        return Set.copyOf(connections.keySet());
    }

    private void sendChangedEvent(UUID userId, String eventName) {
        sendToUser(
                userId,
                () -> SseEmitter.event()
                        .name(eventName)
                        .data("changed"));
    }

    private void sendToUser(
            UUID userId,
            Supplier<SseEmitter.SseEventBuilder> eventSupplier) {
        ConcurrentMap<UUID, SseEmitter> userConnections =
                connections.get(userId);
        if (userConnections == null) {
            return;
        }

        userConnections.forEach((connectionId, emitter) ->
                send(
                        userId,
                        connectionId,
                        emitter,
                        eventSupplier.get()));
    }

    private void send(
            UUID userId,
            UUID connectionId,
            SseEmitter emitter,
            SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            remove(userId, connectionId);
        }
    }

    private void remove(UUID userId, UUID connectionId) {
        connections.computeIfPresent(userId, (ignored, userConnections) -> {
            userConnections.remove(connectionId);
            return userConnections.isEmpty() ? null : userConnections;
        });
    }
}
