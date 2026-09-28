package com.platform.http.api;

import com.platform.http.pipeline.RequestExecutionTrace;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Immutable, implementation-agnostic HTTP response with a typed body. */
public interface HttpResponse<T> {

    int status();

    Map<String, List<String>> headers();

    T body();

    byte[] rawBody();

    HttpRequestSpec request();

    Instant startedAt();

    Instant completedAt();

    Duration duration();

    RequestExecutionTrace trace();

    default boolean isSuccess() {
        return status() >= 200 && status() < 300;
    }

    default boolean isClientError() {
        return status() >= 400 && status() < 500;
    }

    default boolean isServerError() {
        return status() >= 500 && status() < 600;
    }

    default boolean isError() {
        return isClientError() || isServerError();
    }
}
