package com.app.core.utils;

import java.util.Locale;

import org.springframework.util.StringUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TextNormalizationUtils {

    public static String normalizeIdentifier(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
