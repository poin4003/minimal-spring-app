package com.app.features.user.web.support;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.LocaleResolver;

import com.app.config.settings.AppProperties;
import com.app.features.user.schema.result.ProfileResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfilePreferenceCookieService {

    public static final String THEME_COOKIE_NAME = "APP_THEME";

    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    private final AppProperties appProperties;
    private final LocaleResolver localeResolver;

    public void writePreferences(
            HttpServletRequest request,
            HttpServletResponse response,
            ProfileResult profile) {
        localeResolver.setLocale(
                request,
                response,
                profile.getLanguage().toLocale());
        writeTheme(response, profile.isDarkThemeEnabled());
    }

    public void writeTheme(
            HttpServletResponse response,
            boolean darkThemeEnabled) {
        writeCookie(
                response,
                THEME_COOKIE_NAME,
                darkThemeEnabled ? "dark" : "light");
    }

    private void writeCookie(
            HttpServletResponse response,
            String name,
            String value) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                ResponseCookie.from(name, value)
                        .httpOnly(false)
                        .secure(appProperties.getAuth().getCookie().isSecure())
                        .sameSite(appProperties.getAuth().getCookie().getSameSite())
                        .path(appProperties.getAuth().getCookie().getPath())
                        .maxAge(COOKIE_MAX_AGE)
                        .build()
                        .toString());
    }
}
