package com.app.features.ui.web.support;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import com.app.config.security.web.HtmxRequestSupport;
import com.app.features.ui.web.component.view.UiErrorAlertView;
import com.app.features.ui.web.view.ErrorPageView;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class WebErrorPageFactory {

    public ModelAndView build(
            HttpStatus status,
            HttpServletRequest request,
            String error,
            String message) {
        if (HtmxRequestSupport.isHtmxRequest(request)) {
            UiErrorAlertView alert = UiErrorAlertView.builder()
                    .error(error)
                    .message(message)
                    .build();

            return new ModelAndView(
                    "fragments/components/error-alert :: alert (alert=${alert})",
                    Map.of(UiErrorAlertView.ATTRIBUTE, alert),
                    status);
        }

        ErrorPageView page = ErrorPageView.builder()
                .status(status.value())
                .title(resolveTitle(status))
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ModelAndView(
                resolveViewName(status),
                Map.of(ErrorPageView.ATTRIBUTE, page),
                status);
    }

    private String resolveViewName(HttpStatus status) {
        return switch (status) {
            case FORBIDDEN -> "error/403";
            case NOT_FOUND -> "error/404";
            default -> "error/error";
        };
    }

    private String resolveTitle(HttpStatus status) {
        return switch (status) {
            case FORBIDDEN -> "Forbidden";
            case NOT_FOUND -> "Not Found";
            default -> "System Error";
        };
    }
}
