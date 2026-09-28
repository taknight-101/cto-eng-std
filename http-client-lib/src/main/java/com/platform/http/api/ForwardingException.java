package com.platform.http.api;

import java.util.Map;

/** Thrown when a {@code ForwardingPolicy}/{@code ForwardingExecutor} cannot complete a forwarding decision. */
public class ForwardingException extends HttpClientPlatformException {

    public ForwardingException(String message, Throwable cause) {
        super(message, cause, HttpErrorCode.FORWARDING_FAILED, "forwarding", Map.of());
    }
}
