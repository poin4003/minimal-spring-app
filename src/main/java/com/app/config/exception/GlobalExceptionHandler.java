package com.app.config.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.app.core.exception.MyException;
import com.app.core.response.ApiResult;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MyException.class)
    @ResponseBody
    public ResponseEntity<ApiResult<Void>> handleMyException(MyException ex) {
        log.error("MyException [{}]: {}", ex.getError(), ex.getMessage());

        ApiResult<Void> response = ApiResult.error(ex.getError(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatusCode()).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseBody
    public ResponseEntity<ApiResult<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Login failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResult.error("INVALID_CREDENTIALS", "Incorrect email or password!"));
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    @ResponseBody
    public ResponseEntity<ApiResult<Void>> handleValidationException(Exception ex) {
        String errorDetails = "";

        if (ex instanceof MethodArgumentNotValidException e) {
            errorDetails = e.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        } else if (ex instanceof BindException e) {
            errorDetails = e.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        }

        log.warn("Validation Error: {}", errorDetails);
        ApiResult<Void> response = ApiResult.error("INVALID_PARAM", "Invalid data: " + errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            MissingServletRequestPartException.class
    })
    @ResponseBody
    public ResponseEntity<ApiResult<Void>> handleMissingParams(Exception ex) {
        log.warn("Missing Parameter: {}", ex.getMessage());
        ApiResult<Void> response = ApiResult.error("MISSING_PARAM", "Missing param.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({
            AuthorizationDeniedException.class,
            AccessDeniedException.class
    })
    public Object handleAccessDeniedException(Exception ex, HttpServletRequest request) {
        log.warn("[Security] Access Denied: {}", ex.getMessage());

        if (isHtmlRequest(request)) {
            ModelAndView modelAndView = new ModelAndView("error/403");
            modelAndView.setStatus(HttpStatus.FORBIDDEN);
            modelAndView.addObject("path", request.getRequestURI());
            return modelAndView;
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResult.error(
                        "PERMISSION_ERROR",
                        "You are not authorized to perform this action."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        if (isHtmlRequest(request)) {
            ModelAndView modelAndView = new ModelAndView("error/404");
            modelAndView.setStatus(HttpStatus.NOT_FOUND);
            modelAndView.addObject("path", request.getRequestURI());
            return modelAndView;
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResult.error("NOT_FOUND", "Resource not found."));
    }

    @ExceptionHandler(Exception.class)
    public Object handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        log.error("Unknown Internal Error: ", ex);

        if (isHtmlRequest(request)) {
            ModelAndView modelAndView = new ModelAndView("error/error");
            modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            modelAndView.addObject("path", request.getRequestURI());
            return modelAndView;
        }

        ApiResult<Void> response = ApiResult.error("INTERNAL_SERVER_ERROR", "Unknown system error.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private boolean isHtmlRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null
                && accept.contains("text/html")
                && !request.getRequestURI().startsWith("/api/");
    }
}
