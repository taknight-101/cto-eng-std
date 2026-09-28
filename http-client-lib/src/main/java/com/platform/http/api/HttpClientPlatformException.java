package com.platform.http.api;

import java.util.Map;
import java.util.Objects;

/**
 * Root of the HTTP client library's exception hierarchy. Deliberately not shared with
 * {@code security-platform-lib} - see root README architecture-decisions section.
 */
public abstract class HttpClientPlatformException extends RuntimeException {

    private final HttpErrorCode errorCode;
    private final String stage;
    private final Map<String, Object> metadata;

    protected HttpClientPlatformException(
            String message, Throwable cause, HttpErrorCode errorCode, String stage, Map<String, Object> metadata) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.stage = stage == null ? "unknown" : stage;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public HttpErrorCode errorCode() {
        return errorCode;
    }

    public String stage() {
        return stage;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }
}
