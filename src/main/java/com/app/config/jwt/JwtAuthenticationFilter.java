package com.app.config.jwt;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.config.settings.AppProperties;
import com.app.core.security.KeyStoreResult;
import com.app.core.security.UserPrincipal;
import com.app.features.auth.service.KeyStoreService;
import com.app.features.auth.service.impl.UserDetailServiceImpl;

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
    private final UserDetailServiceImpl userDetailsService;
    private final KeyStoreService keyStoreService;
    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);

            if (token != null) {
                UUID keyStoreId = jwtTokenProvider.getKeyStoreIdFromTokenUnverified(token);
                if (keyStoreId != null) {
                    KeyStoreResult keyStore = keyStoreService.getKeyStoreById(keyStoreId);

                    if (keyStore != null)  {
                        UUID userId = jwtTokenProvider.getUserId(token, keyStore.getSigningKey());
                        UserPrincipal userDetails = userDetailsService.loadUserByUserId(userId);
                        userDetails.setKeyStore(keyStore);

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
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
