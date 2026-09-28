package com.platform.security.api;

import java.util.Map;

/** Thrown when an authenticated principal lacks the authority required by policy. */
public class AuthorizationDeniedException extends SecurityPlatformException {

    public AuthorizationDeniedException(String message) {
        super(message, null, SecurityErrorCode.AUTHORIZATION_DENIED, "authorization", Map.of());
    }

    public AuthorizationDeniedException(String message, Map<String, Object> metadata) {
        super(message, null, SecurityErrorCode.AUTHORIZATION_DENIED, "authorization", metadata);
    }
}
