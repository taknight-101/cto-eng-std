package com.platform.http.middleware;

import com.platform.http.api.HttpRequestSpec;
import java.util.Objects;
import java.util.function.Predicate;

/** Immutable metadata describing a registered {@link HttpMiddleware}. */
public record HttpMiddlewareRegistration(
        String name, HttpMiddleware middleware, int order, boolean enabled, Predicate<HttpRequestSpec> condition) {

    public HttpMiddlewareRegistration {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(middleware, "middleware must not be null");
        condition = condition == null ? request -> true : condition;
    }

    public HttpMiddlewareRegistration withEnabled(boolean newEnabled) {
        return new HttpMiddlewareRegistration(name, middleware, order, newEnabled, condition);
    }

    public HttpMiddlewareRegistration withMiddleware(HttpMiddleware newMiddleware) {
        return new HttpMiddlewareRegistration(name, newMiddleware, order, enabled, condition);
    }

    public HttpMiddlewareRegistration withCondition(Predicate<HttpRequestSpec> newCondition) {
        return new HttpMiddlewareRegistration(name, middleware, order, enabled, newCondition);
    }
}
