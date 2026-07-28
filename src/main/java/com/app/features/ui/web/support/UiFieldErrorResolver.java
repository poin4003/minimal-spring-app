package com.app.features.ui.web.support;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import com.app.core.exception.FieldErrorItem;
import com.app.core.exception.MyException;
import com.app.core.i18n.ExceptionMessageResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UiFieldErrorResolver {

    private final ExceptionMessageResolver exceptionMessageResolver;

    public Map<String, String> fromBinding(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return errors;
    }

    public Map<String, String> fromException(MyException ex) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldErrorItem fieldError : exceptionMessageResolver.resolveFieldErrors(ex)) {
            if (fieldError.field() == null || fieldError.field().isBlank()) {
                continue;
            }

            errors.putIfAbsent(fieldError.field(), fieldError.message());
        }

        return errors;
    }
}
