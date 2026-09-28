package com.platform.security.auth;

import com.platform.security.api.AuthenticationOutcome;
import com.platform.security.api.InvalidTokenException;
import com.platform.security.api.JwtAuthenticator;
import com.platform.security.api.SecurityRequestContext;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Default {@link JwtAuthenticator}: extracts a bearer token, delegates validation to a {@link
 * JwtValidator}, and maps claims to authorities via a pluggable {@link Function}.
 */
public final class DefaultJwtAuthenticator implements JwtAuthenticator {

    private final BearerTokenExtractor tokenExtractor;
    private final JwtValidator validator;
    private final Function<JwtClaimsSet, Set<String>> authorityMapper;

    public DefaultJwtAuthenticator(
            BearerTokenExtractor tokenExtractor, JwtValidator validator, Function<JwtClaimsSet, Set<String>> authorityMapper) {
        this.tokenExtractor = tokenExtractor;
        this.validator = validator;
        this.authorityMapper = authorityMapper;
    }

    @Override
    public AuthenticationOutcome authenticate(SecurityRequestContext context) {
        Optional<String> tokenOpt = tokenExtractor.extract(context);
        if (tokenOpt.isEmpty()) {
            return AuthenticationOutcome.notApplicable();
        }
        String token = tokenOpt.get();
        try {
            JwtClaimsSet claims = validator.validate(token);
            Set<String> authorities = authorityMapper.apply(claims);
            return AuthenticationOutcome.success(new JwtAuthenticationToken(claims, token, authorities));
        } catch (InvalidTokenException e) {
            return AuthenticationOutcome.failure(e.errorCode(), e.getMessage(), e);
        }
    }
}
