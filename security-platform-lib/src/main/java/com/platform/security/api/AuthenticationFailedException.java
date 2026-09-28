package com.platform.security.api;

import java.util.Map;

/** Thrown when a token/credential-based authentication attempt fails. */
public class AuthenticationFailedException extends SecurityPlatformException {

    public AuthenticationFailedException(String message, SecurityErrorCode errorCode) {
        this(message, null, errorCode, Map.of());
    }

    public AuthenticationFailedException(
            String message, Throwable cause, SecurityErrorCode errorCode, Map<String, Object> metadata) {
        super(message, cause, errorCode, "authentication", metadata);
    }
}
