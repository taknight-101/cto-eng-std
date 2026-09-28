package com.platform.security.api;

/**
 * Machine-readable classification for {@link SecurityPlatformException}, preferred over
 * parsing exception messages.
 */
public enum SecurityErrorCode {
    MISSING_CREDENTIALS,
    INVALID_TOKEN,
    TOKEN_EXPIRED,
    AUTHENTICATION_FAILED,
    AUTHORIZATION_DENIED,
    MIDDLEWARE_FAILURE,
    CONTEXT_FAILURE,
    UNEXPECTED
}
