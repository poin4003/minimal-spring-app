package com.app.core.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppMessageResolver {

    private final MessageSource messageSource;

    public String get(String messageKey, Object... arguments) {
        return messageSource.getMessage(
                messageKey,
                arguments,
                LocaleContextHolder.getLocale());
    }
}
