package com.platform.http.middleware;

/**
 * A single request/response middleware step, implemented independently from {@code
 * com.platform.security.api.SecurityMiddleware} even though the shape is similar - HTTP
 * middleware operates on requests/responses, not a security lifecycle, and the two libraries
 * do not share a common abstraction.
 */
@FunctionalInterface
public interface HttpMiddleware {

    HttpMiddlewareOutcome handle(com.platform.http.api.HttpRequestSpec request, HttpMiddlewareChain chain)
            throws Exception;

    default HttpMiddlewareOutcome onError(
            com.platform.http.api.HttpRequestSpec request, Throwable error, HttpMiddlewareChain chain)
            throws Exception {
        if (error instanceof RuntimeException re) {
            throw re;
        }
        throw new RuntimeException(error);
    }

    default String name() {
        return getClass().getSimpleName();
    }
}
