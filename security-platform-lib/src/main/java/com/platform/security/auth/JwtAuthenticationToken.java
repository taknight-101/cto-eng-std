package com.platform.security.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security {@code Authentication} produced for a successfully validated JWT.
 * {@code toString()} safety relies on {@link AbstractAuthenticationToken}'s own credential
 * redaction ("Credentials=[PROTECTED]").
 */
public final class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final JwtClaimsSet claims;
    private final String token;

    public JwtAuthenticationToken(JwtClaimsSet claims, String token, Set<String> authorities) {
        super(toGrantedAuthorities(authorities));
        this.claims = claims;
        this.token = token;
        setAuthenticated(true);
    }

    private static Collection<? extends GrantedAuthority> toGrantedAuthorities(Set<String> authorities) {
        return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return claims.subject();
    }

    public JwtClaimsSet claims() {
        return claims;
    }
}
