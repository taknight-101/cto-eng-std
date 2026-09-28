package com.platform.http.api;

import reactor.core.publisher.Mono;

/**
 * Fluent, declarative HTTP client. Public methods return {@link HttpResponse} synchronously
 * (blocking internally); {@link #executeReactive} is the explicit, clearly-named escape hatch
 * for callers that already operate in a reactive/non-blocking context.
 *
 * <p><strong>Blocking:</strong> the sync methods block the calling thread until the response
 * (or timeout) completes. That is safe from a Spring MVC controller/service thread, but must
 * not be called from a WebFlux handler thread - use {@link #executeReactive} there instead.
 */
public interface HttpClient {

    HttpResponse<String> execute(HttpRequestSpec request);

    <T> HttpResponse<T> execute(HttpRequestSpec request, Class<T> responseType);

    <T> HttpResponse<T> execute(HttpRequestSpec request, TypeRef<T> responseType);

    <T> Mono<HttpResponse<T>> executeReactive(HttpRequestSpec request, Class<T> responseType);

    default HttpResponse<String> get(String uri) {
        return execute(HttpRequestSpec.get(uri).build());
    }

    default HttpResponse<String> post(String uri, Object body) {
        return execute(HttpRequestSpec.post(uri).body(body).build());
    }

    default HttpResponse<String> put(String uri, Object body) {
        return execute(HttpRequestSpec.put(uri).body(body).build());
    }

    default HttpResponse<String> patch(String uri, Object body) {
        return execute(HttpRequestSpec.patch(uri).body(body).build());
    }

    default HttpResponse<String> delete(String uri) {
        return execute(HttpRequestSpec.delete(uri).build());
    }
}
