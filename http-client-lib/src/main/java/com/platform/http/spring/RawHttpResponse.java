package com.platform.http.spring;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Raw, transport-level response used only across the internal {@link HttpEngine} adapter boundary. */
public record RawHttpResponse(
        int status, Map<String, List<String>> headers, byte[] rawBody, Instant startedAt, Instant completedAt) {

    public RawHttpResponse {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        rawBody = rawBody == null ? new byte[0] : rawBody.clone();
    }

    public byte[] rawBody() {
        return rawBody.clone();
    }
}
