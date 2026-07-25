package com.app.features.notification.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.constraints.NotNull;

public interface NotificationSseService {

    SseEmitter connect(@NotNull UUID userId);

    void signalChanged(@NotNull UUID userId);

    boolean isOnline(@NotNull UUID userId);

    int countConnections(@NotNull UUID userId);

    Set<UUID> getOnlineUserIds();
}
