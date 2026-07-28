package com.app.config.webmvc;

import java.time.Duration;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import com.app.config.settings.AppProperties;
import com.app.core.enums.AppLanguage;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class LocaleConfig {

    private static final String LANGUAGE_COOKIE_NAME = "APP_LANGUAGE";
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    @Bean
    public LocaleResolver localeResolver(AppProperties appProperties) {
        CookieLocaleResolver resolver =
                new CookieLocaleResolver(LANGUAGE_COOKIE_NAME);

        resolver.setCookiePath(
                appProperties.getAuth().getCookie().getPath());
        resolver.setCookieSecure(
                appProperties.getAuth().getCookie().isSecure());
        resolver.setCookieHttpOnly(false);
        resolver.setCookieSameSite(
                appProperties.getAuth().getCookie().getSameSite());
        resolver.setCookieMaxAge(COOKIE_MAX_AGE);
        resolver.setLanguageTagCompliant(true);
        resolver.setRejectInvalidCookies(false);
        resolver.setDefaultLocaleFunction(
                request -> resolveRequestLocale(request));

        return resolver;
    }

    private Locale resolveRequestLocale(HttpServletRequest request) {
        Enumeration<Locale> requestedLocales = request.getLocales();

        while (requestedLocales.hasMoreElements()) {
            Optional<AppLanguage> language =
                    AppLanguage.fromLocale(requestedLocales.nextElement());

            if (language.isPresent()) {
                return language.get().toLocale();
            }
        }

        return AppLanguage.EN.toLocale();
    }
}
