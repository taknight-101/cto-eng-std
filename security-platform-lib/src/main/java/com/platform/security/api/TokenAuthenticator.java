package com.platform.security.api;

/**
 * Protocol-agnostic contract for turning request credentials into an authentication result.
 * {@link #authenticate} never throws for expected outcomes (missing/invalid/expired
 * credentials) - see {@link AuthenticationOutcome} - reserving exceptions for genuinely
 * unexpected failures (e.g. a JWKS endpoint being unreachable).
 */
@FunctionalInterface
public interface TokenAuthenticator {

    AuthenticationOutcome authenticate(SecurityRequestContext context);
}
