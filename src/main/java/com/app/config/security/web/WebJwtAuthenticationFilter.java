package com.app.config.security.web;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.config.jwt.JwtAccessTokenAuthenticator;
import com.app.config.jwt.JwtAuthenticationToken;
import com.app.features.auth.web.support.AuthCookieService;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebJwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String EXPIRED_ACCESS_TOKEN_ATTRIBUTE =
            WebJwtAuthenticationFilter.class.getName() + ".expiredAccessToken";

    private final JwtAccessTokenAuthenticator jwtAccessTokenAuthenticator;
    private final AuthCookieService authCookieSvc;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateCookie(request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateCookie(HttpServletRequest request) {
        String accessToken = authCookieSvc.readAccessToken(request);
        if (accessToken == null) {
            return;
        }

        try {
            JwtAuthenticationToken authentication =
                    jwtAccessTokenAuthenticator.authenticate(accessToken, request);

            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException exception) {
            request.setAttribute(EXPIRED_ACCESS_TOKEN_ATTRIBUTE, exception);
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            log.debug("Web JWT authentication failed", exception);
        }
    }
}
