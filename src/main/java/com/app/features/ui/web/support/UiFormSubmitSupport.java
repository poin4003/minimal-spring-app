package com.app.features.ui.web.support;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import com.app.core.exception.MyException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UiFormSubmitSupport {

    private final UiFieldErrorResolver uiFieldErrorResolver;

    public UiFormSubmitResult submit(ThrowingAction action) {
        try {
            action.run();
            return UiFormSubmitResult.ok();
        } catch (MyException ex) {
            Map<String, String> fieldErrors = uiFieldErrorResolver.fromException(ex);
            if (fieldErrors.isEmpty()) {
                throw ex;
            }

            return UiFormSubmitResult.fail(fieldErrors);
        }
    }

    @FunctionalInterface
    public interface ThrowingAction {
        void run();
    }

    public UiFormSubmitResult submit(BindingResult bindingResult, ThrowingAction action) {
        Map<String, String> bindingErrors = uiFieldErrorResolver.fromBinding(bindingResult);
        if (!bindingErrors.isEmpty()) {
            return UiFormSubmitResult.fail(bindingErrors);
        }

        return submit(action);
    }
}
