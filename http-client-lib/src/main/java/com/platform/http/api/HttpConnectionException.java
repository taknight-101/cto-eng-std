package com.platform.http.api;

import java.util.Map;

/** Thrown for transport-level failures: connection refused, DNS, timeout, TLS. */
public class HttpConnectionException extends HttpClientPlatformException {

    public HttpConnectionException(String message, Throwable cause, HttpErrorCode errorCode) {
        super(message, cause, errorCode, "connection", Map.of());
    }
}
