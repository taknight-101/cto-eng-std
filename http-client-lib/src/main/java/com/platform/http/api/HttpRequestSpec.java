package com.platform.http.api;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Immutable, inspectable HTTP request. Never tied to a particular HTTP client implementation.
 *
 * <pre>{@code
 * HttpRequestSpec request = HttpRequestSpec.post("/orders")
 *         .header("X-Tenant", tenantId)
 *         .body(order)
 *         .timeout(Duration.ofSeconds(5))
 *         .build();
 * }</pre>
 */
public interface HttpRequestSpec {

    static Builder builder() {
        return new com.platform.http.request.DefaultHttpRequestSpec.Builder();
    }

    static Builder get(String uri) {
        return builder().method(HttpMethodType.GET).uri(uri);
    }

    static Builder post(String uri) {
        return builder().method(HttpMethodType.POST).uri(uri);
    }

    static Builder put(String uri) {
        return builder().method(HttpMethodType.PUT).uri(uri);
    }

    static Builder patch(String uri) {
        return builder().method(HttpMethodType.PATCH).uri(uri);
    }

    static Builder delete(String uri) {
        return builder().method(HttpMethodType.DELETE).uri(uri);
    }

    HttpMethodType method();

    String uri();

    Map<String, List<String>> queryParams();

    Map<String, List<String>> headers();

    Map<String, String> cookies();

    RequestBody body();

    String acceptType();

    Duration timeout();

    String correlationId();

    Map<String, Object> metadata();

    /** Whether this request is safe to retry automatically (see {@code RetryPolicy}). */
    boolean idempotent();

    /** Full URI including query parameters, as it will be sent on the wire. */
    String resolvedUri();

    HttpRequestSpec withHeader(String name, String value);

    HttpRequestSpec withCorrelationId(String correlationId);

    HttpRequestSpec withBody(RequestBody body);

    interface Builder {
        Builder method(HttpMethodType method);

        Builder uri(String uri);

        Builder queryParam(String name, String value);

        Builder header(String name, String value);

        Builder cookie(String name, String value);

        Builder body(Object jsonBody);

        Builder rawBody(byte[] bytes, String contentType);

        Builder accept(String acceptType);

        Builder timeout(Duration timeout);

        Builder correlationId(String correlationId);

        Builder metadata(String key, Object value);

        Builder idempotent(boolean idempotent);

        HttpRequestSpec build();
    }
}
