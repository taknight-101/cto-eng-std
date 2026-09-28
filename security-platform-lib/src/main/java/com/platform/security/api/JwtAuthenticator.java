package com.platform.security.api;

/**
 * Specialization marker for JWT-based {@link TokenAuthenticator}s, used for Spring bean
 * lookup/qualification. The default implementation delegates signature verification and
 * standard claim validation to Spring Security's own {@code JwtDecoder} (itself backed by
 * Nimbus JOSE+JWT) rather than re-implementing cryptography.
 */
public interface JwtAuthenticator extends TokenAuthenticator {
}
