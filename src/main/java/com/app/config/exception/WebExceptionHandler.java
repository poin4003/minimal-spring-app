package com.app.config.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.app.core.exception.MyException;
import com.app.features.ui.web.support.WebErrorPageFactory;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(basePackages = {
        "com.app.features.auth.web.controller",
        "com.app.features.ui.web.controller",
        "com.app.features.user.web.controller",
        "com.app.features.rbac.web.controller",
        "com.app.features.cronjob.web.controller",
        "com.app.features.media.web.controller"
})
public class WebExceptionHandler {

    private final WebErrorPageFactory webErrorPageFactory;

    @ExceptionHandler({
            AuthorizationDeniedException.class,
            AccessDeniedException.class
    })
    public ModelAndView handleAccessDeniedException(Exception ex, HttpServletRequest request) {
        log.warn("[Security] Access Denied: {}", ex.getMessage());

        return webErrorPageFactory.build(
                HttpStatus.FORBIDDEN,
                request,
                "PERMISSION_ERROR",
                "You are not authorized to perform this action.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        return webErrorPageFactory.build(
                HttpStatus.NOT_FOUND,
                request,
                "NOT_FOUND",
                "Resource not found.");
    }

    @ExceptionHandler(MyException.class)
    public ModelAndView handleMyException(MyException ex, HttpServletRequest request) {
        log.error("Web MyException [{}]: {}", ex.getError(), ex.getMessage());

        return webErrorPageFactory.build(
                HttpStatus.valueOf(ex.getHttpStatusCode()),
                request,
                ex.getError(),
                ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        log.warn("Multipart upload size exceeded: {}", ex.getMessage());

        return webErrorPageFactory.build(
                HttpStatus.CONTENT_TOO_LARGE,
                request,
                "PAYLOAD_TOO_LARGE",
                "Uploaded file exceeds the multipart limit. Use chunk upload for large media.");
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public ModelAndView handleValidationException(Exception ex, HttpServletRequest request) {
        log.warn("Web validation error: {}", ex.getMessage());

        return webErrorPageFactory.build(
                HttpStatus.BAD_REQUEST,
                request,
                "COMMON_VALIDATION_ERROR",
                "Invalid request data.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        log.error("Unknown Internal Error: ", ex);

        return webErrorPageFactory.build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                request,
                "INTERNAL_SERVER_ERROR",
                "Unknown system error.");
    }
}
