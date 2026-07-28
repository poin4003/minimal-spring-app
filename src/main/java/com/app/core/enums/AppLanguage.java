package com.app.core.enums;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AppLanguage {
    EN("en"),
    VI("vi");

    private static final Map<String, AppLanguage> LANGUAGE_BY_TAG;

    static {
        Map<String, AppLanguage> languages = new HashMap<>();
        for (AppLanguage language : values()) {
            languages.put(language.languageTag, language);
        }
        LANGUAGE_BY_TAG = Map.copyOf(languages);
    }

    private final String languageTag;

    public Locale toLocale() {
        return Locale.forLanguageTag(languageTag);
    }

    public static Optional<AppLanguage> fromLocale(Locale locale) {
        if (locale == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                LANGUAGE_BY_TAG.get(
                        locale.getLanguage().toLowerCase(Locale.ROOT)));
    }
}
