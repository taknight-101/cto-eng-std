package com.platform.security.api;

import java.util.Map;
import java.util.Objects;

/**
 * Root of the security library's exception hierarchy. Deliberately not shared with
 * {@code http-client-lib} (see root README architecture-decisions section) to avoid an
 * unnecessary compile-time coupling between the two independent libraries.
 *
 * <p>Carries a machine-readable {@link SecurityErrorCode}, the pipeline stage where the
 * failure occurred, and arbitrary redaction-safe metadata, so consumers do not need to parse
 * exception messages to react programmatically.
 */
public abstract class SecurityPlatformException extends RuntimeException {

    private final SecurityErrorCode errorCode;
    private final String stage;
    private final Map<String, Object> metadata;

    protected SecurityPlatformException(
            String message,
            Throwable cause,
            SecurityErrorCode errorCode,
            String stage,
            Map<String, Object> metadata) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.stage = stage == null ? "unknown" : stage;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public SecurityErrorCode errorCode() {
        return errorCode;
    }

    public String stage() {
        return stage;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }
}
