package com.platform.http.request;

import com.platform.http.api.HttpMethodType;
import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.RequestBody;
import com.platform.http.internal.CorrelationIds;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Sole implementation of {@link HttpRequestSpec}. */
public record DefaultHttpRequestSpec(
        HttpMethodType method,
        String uri,
        Map<String, List<String>> queryParams,
        Map<String, List<String>> headers,
        Map<String, String> cookies,
        RequestBody body,
        String acceptType,
        Duration timeout,
        String correlationId,
        Map<String, Object> metadata,
        boolean idempotent) implements HttpRequestSpec {

    private static final Set<HttpMethodType> NATURALLY_IDEMPOTENT =
            Set.of(HttpMethodType.GET, HttpMethodType.HEAD, HttpMethodType.OPTIONS, HttpMethodType.PUT, HttpMethodType.DELETE);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    public DefaultHttpRequestSpec {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(uri, "uri must not be null");
        // LinkedHashMap + unmodifiableMap (not Map.copyOf) to preserve caller-supplied order:
        // Map.copyOf's iteration order is randomized per-JVM-run, which would make
        // resolvedUri() and header ordering non-deterministic across runs.
        queryParams = queryParams == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(queryParams));
        headers = headers == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        cookies = cookies == null ? Map.of() : Map.copyOf(cookies);
        body = body == null ? RequestBody.empty() : body;
        timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        correlationId = correlationId == null ? CorrelationIds.newId() : correlationId;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    @Override
    public String resolvedUri() {
        if (queryParams.isEmpty()) {
            return uri;
        }
        StringBuilder sb = new StringBuilder(uri);
        sb.append(uri.contains("?") ? '&' : '?');
        boolean first = true;
        for (var entry : queryParams.entrySet()) {
            for (String value : entry.getValue()) {
                if (!first) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                first = false;
            }
        }
        return sb.toString();
    }

    @Override
    public HttpRequestSpec withHeader(String name, String value) {
        Map<String, List<String>> updated = new LinkedHashMap<>(headers);
        updated.put(name, List.of(value));
        return new DefaultHttpRequestSpec(method, uri, queryParams, updated, cookies, body, acceptType, timeout,
                correlationId, metadata, idempotent);
    }

    @Override
    public HttpRequestSpec withCorrelationId(String newCorrelationId) {
        return new DefaultHttpRequestSpec(method, uri, queryParams, headers, cookies, body, acceptType, timeout,
                newCorrelationId, metadata, idempotent);
    }

    @Override
    public HttpRequestSpec withBody(RequestBody newBody) {
        return new DefaultHttpRequestSpec(method, uri, queryParams, headers, cookies, newBody, acceptType, timeout,
                correlationId, metadata, idempotent);
    }

    public static final class Builder implements HttpRequestSpec.Builder {
        private HttpMethodType method = HttpMethodType.GET;
        private String uri = "";
        private final Map<String, List<String>> queryParams = new LinkedHashMap<>();
        private final Map<String, List<String>> headers = new LinkedHashMap<>();
        private final Map<String, String> cookies = new LinkedHashMap<>();
        private RequestBody body = RequestBody.empty();
        private String acceptType = "application/json";
        private Duration timeout = DEFAULT_TIMEOUT;
        private String correlationId;
        private final Map<String, Object> metadata = new LinkedHashMap<>();
        private Boolean idempotentOverride;

        @Override
        public Builder method(HttpMethodType method) {
            this.method = Objects.requireNonNull(method);
            return this;
        }

        @Override
        public Builder uri(String uri) {
            this.uri = Objects.requireNonNull(uri);
            return this;
        }

        @Override
        public Builder queryParam(String name, String value) {
            queryParams.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(value);
            return this;
        }

        @Override
        public Builder header(String name, String value) {
            headers.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(value);
            return this;
        }

        @Override
        public Builder cookie(String name, String value) {
            cookies.put(name, value);
            return this;
        }

        @Override
        public Builder body(Object jsonBody) {
            this.body = RequestBody.json(jsonBody);
            return this;
        }

        @Override
        public Builder rawBody(byte[] bytes, String contentType) {
            this.body = RequestBody.raw(bytes, contentType);
            return this;
        }

        @Override
        public Builder accept(String acceptType) {
            this.acceptType = acceptType;
            return this;
        }

        @Override
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        @Override
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        @Override
        public Builder metadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        @Override
        public Builder idempotent(boolean idempotent) {
            this.idempotentOverride = idempotent;
            return this;
        }

        @Override
        public HttpRequestSpec build() {
            boolean idempotent = idempotentOverride != null ? idempotentOverride : NATURALLY_IDEMPOTENT.contains(method);
            return new DefaultHttpRequestSpec(method, uri, queryParams, headers, cookies, body, acceptType, timeout,
                    correlationId, metadata, idempotent);
        }
    }
}
