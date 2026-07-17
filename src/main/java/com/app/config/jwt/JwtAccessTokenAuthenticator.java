package com.app.config.jwt;

import java.util.UUID;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import com.app.core.security.KeyStoreResult;
import com.app.core.security.UserPrincipal;
import com.app.features.auth.security.UserPrincipalFactory;
import com.app.features.auth.service.KeyStoreService;
import com.app.features.user.enums.UserStatusEnum;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAccessTokenAuthenticator {

    private final JwtTokenProvider jwtTokenProvider;
    private final KeyStoreService keyStoreService;
    private final UserPrincipalFactory userPrincipalFactory;

    public JwtAuthenticationToken authenticate(
            String token,
            HttpServletRequest request) throws Exception {
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

        JwtAccessPayload payload = verifiedToken.getPayload();
        if (!verifiedToken.getUserId().equals(keyStore.getUserId())
                || payload.getUserEmail() == null
                || payload.getStatus() != UserStatusEnum.ACTIVE) {
            return null;
        }

        UserPrincipal principal = userPrincipalFactory.fromToken(verifiedToken, keyStore);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(principal);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }
}
