package com.platform.security.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;

/** Unauthenticated placeholder token carrying a raw bearer token, used by {@link AuthenticationProviderAdapter}. */
public final class BearerAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;

    public BearerAuthenticationToken(String token) {
        super(List.of());
        this.token = token;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }
}
