package com.platform.security.auth;

import com.platform.security.api.InvalidTokenException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Set;
import java.util.function.Function;

/**
 * Escape hatch: bridges {@link JwtValidator} into a plain Spring Security {@link
 * AuthenticationProvider}, for applications that want the platform's JWT validation logic
 * without adopting the {@code SecurityPipeline}/middleware model.
 */
public final class AuthenticationProviderAdapter implements AuthenticationProvider {

    private final JwtValidator validator;
    private final Function<JwtClaimsSet, Set<String>> authorityMapper;

    public AuthenticationProviderAdapter(JwtValidator validator, Function<JwtClaimsSet, Set<String>> authorityMapper) {
        this.validator = validator;
        this.authorityMapper = authorityMapper;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getCredentials();
        try {
            JwtClaimsSet claims = validator.validate(token);
            return new JwtAuthenticationToken(claims, token, authorityMapper.apply(claims));
        } catch (InvalidTokenException e) {
            throw new BadCredentialsException("Invalid bearer token", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
