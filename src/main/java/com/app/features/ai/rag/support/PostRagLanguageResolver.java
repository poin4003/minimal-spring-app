package com.app.features.ai.rag.support;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.app.core.enums.AppLanguage;

@Component
public class PostRagLanguageResolver {

    public AppLanguage resolve(Locale locale) {
        return AppLanguage.fromLocale(locale)
                .orElse(AppLanguage.EN);
    }

    public String getResponseLanguageName(AppLanguage language) {
        return switch (language) {
            case EN -> "English";
            case VI -> "Vietnamese";
        };
    }
}
