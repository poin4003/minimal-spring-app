package com.app.features.auth.web.support;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.app.config.settings.AppProperties;
import com.app.features.auth.schema.result.LoginResult;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthCookieService {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private final AppProperties appProperties;

    public void writeAuthenticationCookies(HttpServletResponse response, LoginResult tokens) {
        writeCookie(
                response,
                appProperties.getAuth().getCookie().getAccessTokenName(),
                tokens.getAccessToken());
        writeCookie(
                response,
                appProperties.getAuth().getCookie().getRefreshTokenName(),
                tokens.getRefreshToken());
    }

    public String readAccessToken(HttpServletRequest request) {
        return readCookie(
                request,
                appProperties.getAuth().getCookie().getAccessTokenName());
    }

    public String readRefreshToken(HttpServletRequest request) {
        return readCookie(
                request,
                appProperties.getAuth().getCookie().getRefreshTokenName());
    }

    public void clearAuthenticationCookies(HttpServletResponse response) {
        expireCookie(response, appProperties.getAuth().getCookie().getAccessTokenName(), true);
        expireCookie(response, appProperties.getAuth().getCookie().getRefreshTokenName(), true);
        expireCookie(response, CSRF_COOKIE_NAME, false);
        expireCookie(response, SESSION_COOKIE_NAME, true);
    }

    private void writeCookie(HttpServletResponse response, String cookieName, String value) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(appProperties.getAuth().getCookie().isSecure())
                .sameSite(appProperties.getAuth().getCookie().getSameSite())
                .path(appProperties.getAuth().getCookie().getPath())
                .build()
                .toString());
    }

    private String readCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
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
