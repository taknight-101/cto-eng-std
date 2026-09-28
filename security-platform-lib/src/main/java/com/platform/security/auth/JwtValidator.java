package com.platform.security.auth;

import com.platform.security.api.InvalidTokenException;

/**
 * Delegates signature verification, standard claim validation (expiry, not-before) and claim
 * parsing to a mature implementation - never re-implemented in-house.
 */
@FunctionalInterface
public interface JwtValidator {

    /**
     * @throws InvalidTokenException if the token is malformed, has an invalid signature, is
     *                               expired, or fails issuer/audience validation
     */
    JwtClaimsSet validate(String token);
}
