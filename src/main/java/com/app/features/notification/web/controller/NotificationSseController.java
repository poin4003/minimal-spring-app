package com.app.features.notification.web.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.app.core.security.UserPrincipal;
import com.app.features.notification.service.NotificationSseService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/notifications")
public class NotificationSseController {

    private final NotificationSseService notificationSseSvc;

    @GetMapping(
            path = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletResponse response) {
        response.setHeader(
                HttpHeaders.CACHE_CONTROL,
                "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");

        return notificationSseSvc.connect(currentUser.getUserId());
    }
}
