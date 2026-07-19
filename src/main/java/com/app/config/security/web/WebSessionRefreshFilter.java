package com.app.config.security.web;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.config.jwt.JwtAccessTokenAuthenticator;
import com.app.config.jwt.JwtAuthenticationToken;
import com.app.features.auth.schema.payload.RefreshTokenPayload;
import com.app.features.auth.schema.result.LoginResult;
import com.app.features.auth.service.AuthService;
import com.app.features.auth.web.support.AuthCookieService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSessionRefreshFilter extends OncePerRequestFilter {

    private final AuthService authSvc;
    private final AuthCookieService authCookieSvc;
    private final JwtAccessTokenAuthenticator jwtAccessTokenAuthenticator;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (shouldRefresh(request)) {
            refreshSession(request, response);
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldRefresh(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);

        return SecurityContextHolder.getContext().getAuthentication() == null
                && request.getAttribute(WebJwtAuthenticationFilter.EXPIRED_ACCESS_TOKEN_ATTRIBUTE) != null
                && accept != null
                && accept.contains(MediaType.TEXT_HTML_VALUE);
    }

    private void refreshSession(
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String refreshToken = authCookieSvc.readRefreshToken(request);
            if (refreshToken == null) {
                return;
            }

            RefreshTokenPayload payload = new RefreshTokenPayload();
            payload.setRefreshToken(refreshToken);

            LoginResult tokens = authSvc.refreshToken(payload);
            JwtAuthenticationToken authentication = jwtAccessTokenAuthenticator.authenticate(
                    tokens.getAccessToken(),
                    request);

            if (authentication == null) {
                throw new IllegalStateException("Refreshed access token could not be authenticated.");
            }

            authCookieSvc.writeAuthenticationCookies(response, tokens);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            authCookieSvc.clearAuthenticationCookies(response);
            log.debug("Web session refresh failed", exception);
        }
    }
}
