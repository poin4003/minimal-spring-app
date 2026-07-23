package com.app.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.app.core.exception.MyException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExceptionLogService {

    public void log(MyException ex, HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (ex.getHttpStatusCode() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error(
                    "Request failed [{}] {} {}: {}",
                    ex.getError(),
                    method,
                    path,
                    ex.getMessage(),
                    ex);
            return;
        }

        if (isSecurityOrRateLimitError(ex.getHttpStatusCode())) {
            log.warn(
                    "Request rejected [{}] {} {}: {}",
                    ex.getError(),
                    method,
                    path,
                    ex.getMessage());
            return;
        }

        log.info(
                "Request handled [{}] {} {}: {}",
                ex.getError(),
                method,
                path,
                ex.getMessage());
    }

    public void logUnexpected(Exception ex, HttpServletRequest request) {
        log.error(
                "Unexpected error {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex);
    }

    private boolean isSecurityOrRateLimitError(int httpStatusCode) {
        return httpStatusCode == HttpStatus.UNAUTHORIZED.value()
                || httpStatusCode == HttpStatus.FORBIDDEN.value()
                || httpStatusCode == HttpStatus.TOO_MANY_REQUESTS.value();
    }
}
