package com.app.features.ui.web.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UiDateTimeFormatter {

    public String format(LocalDateTime value) {
        if (value == null) {
            return "";
        }

        return value.format(DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(LocaleContextHolder.getLocale()));
    }
}
