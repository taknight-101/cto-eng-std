package com.platform.http.response;

import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.HttpResponse;
import com.platform.http.pipeline.RequestExecutionTrace;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Sole implementation of {@link HttpResponse}. */
public record DefaultHttpResponse<T>(
        int status,
        Map<String, List<String>> headers,
        T body,
        byte[] rawBody,
        HttpRequestSpec request,
        Instant startedAt,
        Instant completedAt,
        RequestExecutionTrace trace) implements HttpResponse<T> {

    public DefaultHttpResponse {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        rawBody = rawBody == null ? new byte[0] : rawBody.clone();
        trace = trace == null ? RequestExecutionTrace.empty(request.correlationId()) : trace;
    }

    @Override
    public byte[] rawBody() {
        return rawBody.clone();
    }

    @Override
    public Duration duration() {
        return Duration.between(startedAt, completedAt);
    }
}
