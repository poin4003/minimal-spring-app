package com.app.features.notification.web.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.config.settings.AppProperties;
import com.app.config.security.web.HtmxRequestSupport;
import com.app.core.security.UserPrincipal;
import com.app.features.notification.service.NotificationService;
import com.app.features.notification.web.support.NotificationInboxViewFactory;
import com.app.features.notification.web.view.NotificationInboxView;
import com.app.features.notification.web.view.NotificationUnreadCountView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.notification-path:/notifications}")
public class NotificationInboxController {

    private static final String NOTIFICATION_CHANGED_EVENT =
            "notification:changed";

    private final AppProperties appProperties;
    private final NotificationService notificationSvc;
    private final NotificationInboxViewFactory notificationInboxViewFactory;

    @GetMapping("/unread-count")
    public String unreadCount(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Model model) {
        model.addAttribute(
                NotificationUnreadCountView.ATTRIBUTE,
                notificationInboxViewFactory.buildUnreadCount(
                        currentUser.getUserId()));
        return "notification/fragments/inbox :: unreadCount"
                + " (count=${notificationUnreadCount})";
    }

    @GetMapping("/inbox")
    public String inbox(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Model model) {
        return renderInbox(currentUser.getUserId(), model);
    }

    @PostMapping("/{notificationId}/read")
    public String markAsRead(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID notificationId,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        notificationSvc.markAsRead(
                currentUser.getUserId(),
                notificationId);
        if (!HtmxRequestSupport.isHtmxRequest(request)) {
            return HtmxRequestSupport.redirectView(
                    request,
                    response,
                    appProperties.getUi().getSocialPath());
        }

        HtmxRequestSupport.trigger(response, NOTIFICATION_CHANGED_EVENT);
        return renderInbox(currentUser.getUserId(), model);
    }

    @PostMapping("/read-all")
    public String markAllAsRead(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        notificationSvc.markAllAsRead(currentUser.getUserId());
        if (!HtmxRequestSupport.isHtmxRequest(request)) {
            return HtmxRequestSupport.redirectView(
                    request,
                    response,
                    appProperties.getUi().getSocialPath());
        }

        HtmxRequestSupport.trigger(response, NOTIFICATION_CHANGED_EVENT);
        return renderInbox(currentUser.getUserId(), model);
    }

    private String renderInbox(UUID recipientId, Model model) {
        model.addAttribute(
                NotificationInboxView.ATTRIBUTE,
                notificationInboxViewFactory.buildInbox(recipientId));
        return "notification/fragments/inbox :: inbox"
                + " (inbox=${notificationInbox})";
    }
}
