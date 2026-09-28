package com.platform.http.events;

import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.HttpResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** A single point-in-time HTTP lifecycle occurrence, published through the {@link HttpEventDrill}. */
public record HttpEvent(
        HttpEventType type,
        Instant timestamp,
        String correlationId,
        HttpRequestSpec request,
        HttpResponse<?> response,
        Throwable cause,
        String stage,
        Duration duration,
        Map<String, Object> metadata) {

    public HttpEvent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public Optional<HttpResponse<?>> responseOptional() {
        return Optional.ofNullable(response);
    }

    public Optional<Throwable> causeOptional() {
        return Optional.ofNullable(cause);
    }

    public static HttpEvent of(HttpEventType type, HttpRequestSpec request, String stage, Map<String, Object> metadata) {
        return new HttpEvent(type, Instant.now(), request.correlationId(), request, null, null, stage, null, metadata);
    }

    public static HttpEvent of(
            HttpEventType type, HttpRequestSpec request, Throwable cause, String stage, Map<String, Object> metadata) {
        return new HttpEvent(type, Instant.now(), request.correlationId(), request, null, cause, stage, null, metadata);
    }

    public static HttpEvent of(
            HttpEventType type, HttpRequestSpec request, HttpResponse<?> response, String stage, Duration duration,
            Map<String, Object> metadata) {
        return new HttpEvent(type, Instant.now(), request.correlationId(), request, response, null, stage, duration, metadata);
    }
}
