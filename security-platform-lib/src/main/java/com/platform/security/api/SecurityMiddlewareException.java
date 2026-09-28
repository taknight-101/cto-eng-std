package com.platform.security.api;

import java.util.Map;

/** Wraps an unexpected exception thrown by user-registered {@link SecurityMiddleware}. */
public class SecurityMiddlewareException extends SecurityPlatformException {

    public SecurityMiddlewareException(String message, Throwable cause, String middlewareName) {
        super(message, cause, SecurityErrorCode.MIDDLEWARE_FAILURE, "middleware",
                Map.of("middleware", middlewareName));
    }
}
