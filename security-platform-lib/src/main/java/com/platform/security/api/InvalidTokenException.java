package com.platform.security.api;

import java.util.Map;

/** Thrown when a token is structurally invalid, has a bad signature, or is expired. */
public class InvalidTokenException extends AuthenticationFailedException {

    public InvalidTokenException(String message, SecurityErrorCode errorCode) {
        super(message, errorCode);
    }

    public InvalidTokenException(
            String message, Throwable cause, SecurityErrorCode errorCode, Map<String, Object> metadata) {
        super(message, cause, errorCode, metadata);
    }
}
