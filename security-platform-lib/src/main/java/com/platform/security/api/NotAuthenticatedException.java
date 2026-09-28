package com.platform.security.api;

import java.util.Map;

/** Thrown by {@link SecurityContextAccessor#requireAuthentication()} when nothing is authenticated. */
public class NotAuthenticatedException extends SecurityPlatformException {

    public NotAuthenticatedException() {
        super("No authenticated principal is present in the current security context",
                null, SecurityErrorCode.MISSING_CREDENTIALS, "context-access", Map.of());
    }
}
