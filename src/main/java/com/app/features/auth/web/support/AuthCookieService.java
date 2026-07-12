package com.app.features.auth.web.support;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.app.config.settings.AppProperties;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthCookieService {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private final AppProperties appProperties;

    public void writeAuthenticationCookie(HttpServletResponse response, String accessToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(
                appProperties.getAuth().getCookie().getAccessTokenName(),
                accessToken)
                .httpOnly(true)
                .secure(appProperties.getAuth().getCookie().isSecure())
                .sameSite(appProperties.getAuth().getCookie().getSameSite())
                .path(appProperties.getAuth().getCookie().getPath())
                .build()
                .toString());
    }

    public void clearAuthenticationCookies(HttpServletResponse response) {
        expireCookie(response, appProperties.getAuth().getCookie().getAccessTokenName(), true);
        expireCookie(response, CSRF_COOKIE_NAME, false);
        expireCookie(response, SESSION_COOKIE_NAME, true);
    }

    private void expireCookie(HttpServletResponse response, String cookieName, boolean httpOnly) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(cookieName, "")
                .httpOnly(httpOnly)
                .secure(appProperties.getAuth().getCookie().isSecure())
                .sameSite(appProperties.getAuth().getCookie().getSameSite())
                .path(appProperties.getAuth().getCookie().getPath())
                .maxAge(Duration.ZERO)
                .build()
                .toString());
    }
}
