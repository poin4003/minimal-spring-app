package com.app.features.auth.security;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.app.config.jwt.JwtAccessPayload;
import com.app.config.jwt.JwtVerifiedAccessToken;
import com.app.core.security.KeyStoreResult;
import com.app.core.security.UserPrincipal;

@Component
public class UserPrincipalFactory {

    public UserPrincipal fromToken(JwtVerifiedAccessToken verifiedToken, KeyStoreResult keyStore) {
        JwtAccessPayload payload = verifiedToken.getPayload();

        return UserPrincipal.builder()
                .userId(verifiedToken.getUserId())
                .email(payload.getUserEmail())
                .status(payload.getStatus())
                .keyStore(keyStore)
                .authorities(buildAuthorities(payload))
                .build();
    }

    private Set<GrantedAuthority> buildAuthorities(JwtAccessPayload payload) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        addAuthorities(authorities, payload.getRoles());
        addAuthorities(authorities, payload.getPermissions());

        return authorities;
    }

    private void addAuthorities(Set<GrantedAuthority> authorities, Set<String> authorityKeys) {
        if (authorityKeys == null) {
            return;
        }

        for (String authorityKey : authorityKeys) {
            authorities.add(new SimpleGrantedAuthority(authorityKey));
        }
    }
}
