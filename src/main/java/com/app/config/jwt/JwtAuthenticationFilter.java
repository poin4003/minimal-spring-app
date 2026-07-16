package com.app.config.jwt;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.config.settings.AppProperties;
import com.app.core.security.KeyStoreResult;
import com.app.core.security.UserPrincipal;
import com.app.features.auth.security.UserPrincipalFactory;
import com.app.features.auth.service.KeyStoreService;
import com.app.features.user.enums.UserStatusEnum;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final KeyStoreService keyStoreService;
    private final UserPrincipalFactory userPrincipalFactory;
    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserPrincipal principal = authenticate(token);

                if (principal != null) {
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(principal);

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            log.debug("Access token authentication failed", exception);
        }

        filterChain.doFilter(request, response);
    }

    private UserPrincipal authenticate(String token) throws Exception {
        UUID keyStoreId = jwtTokenProvider.getKeyStoreIdFromTokenUnverified(token);
        if (keyStoreId == null) {
            return null;
        }

        KeyStoreResult keyStore = keyStoreService.getKeyStoreById(keyStoreId);
        if (keyStore == null) {
            return null;
        }

        JwtVerifiedAccessToken verifiedToken = jwtTokenProvider.getVerifiedAccessToken(
                token,
                keyStore.getSigningKey());

        if (!verifiedToken.getUserId().equals(keyStore.getUserId())) {
            return null;
        }

        JwtAccessPayload payload = verifiedToken.getPayload();
        if (payload.getUserEmail() == null || payload.getStatus() != UserStatusEnum.ACTIVE) {
            return null;
        }

        return userPrincipalFactory.fromToken(verifiedToken, keyStore);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        String accessTokenCookieName = appProperties.getAuth().getCookie().getAccessTokenName();
        for (Cookie cookie : cookies) {
            if (accessTokenCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
