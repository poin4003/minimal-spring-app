package com.app.config.jwt;

import java.io.Serial;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import com.app.core.security.UserPrincipal;

public final class JwtAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UserPrincipal principal;

    public JwtAuthenticationToken(UserPrincipal principal) {
        super(principal.getAuthorities());
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public UserPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return null;
    }
}
