package com.platform.http.middleware;

import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.HttpResponse;

/** Result of a single {@link HttpMiddleware} step: continue with a (possibly transformed) request, or short-circuit with a response. */
public sealed interface HttpMiddlewareOutcome {

    static HttpMiddlewareOutcome proceed(HttpRequestSpec request) {
        return new Continue(request);
    }

    static HttpMiddlewareOutcome respond(HttpResponse<?> response) {
        return new ShortCircuit(response);
    }

    record Continue(HttpRequestSpec request) implements HttpMiddlewareOutcome {
    }

    record ShortCircuit(HttpResponse<?> response) implements HttpMiddlewareOutcome {
    }
}
