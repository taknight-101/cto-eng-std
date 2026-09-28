package com.platform.security.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A single point-in-time security lifecycle occurrence, published through the {@link
 * SecurityEventDrill}.
 *
 * <p>{@link #metadata()} and {@link #context()} must never carry raw secrets; producers are
 * expected to have already redacted sensitive values (see
 * {@code com.platform.security.internal.Redaction}) before constructing an event.
 */
public record SecurityEvent(
        SecurityEventType type,
        Instant timestamp,
        String correlationId,
        SecurityRequestContext context,
        Throwable cause,
        String stage,
        Map<String, Object> metadata) {

    public SecurityEvent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public Optional<Throwable> causeOptional() {
        return Optional.ofNullable(cause);
    }

    public static SecurityEvent of(
            SecurityEventType type, SecurityRequestContext context, String stage, Map<String, Object> metadata) {
        return new SecurityEvent(type, Instant.now(), context.correlationId(), context, null, stage, metadata);
    }

    public static SecurityEvent of(
            SecurityEventType type,
            SecurityRequestContext context,
            Throwable cause,
            String stage,
            Map<String, Object> metadata) {
        return new SecurityEvent(type, Instant.now(), context.correlationId(), context, cause, stage, metadata);
    }
}
