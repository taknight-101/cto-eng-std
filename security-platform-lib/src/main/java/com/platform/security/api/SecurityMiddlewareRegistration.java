package com.platform.security.api;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Immutable metadata describing a registered {@link SecurityMiddleware}: its name (used for
 * {@code registerBefore}/{@code registerAfter}/{@code replace}/{@code remove}), its execution
 * order, whether it is currently enabled, and an optional per-request condition.
 */
public record SecurityMiddlewareRegistration(
        String name,
        SecurityMiddleware middleware,
        int order,
        boolean enabled,
        Predicate<SecurityRequestContext> condition) {

    public SecurityMiddlewareRegistration {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(middleware, "middleware must not be null");
        condition = condition == null ? ctx -> true : condition;
    }

    public SecurityMiddlewareRegistration withEnabled(boolean newEnabled) {
        return new SecurityMiddlewareRegistration(name, middleware, order, newEnabled, condition);
    }

    public SecurityMiddlewareRegistration withOrder(int newOrder) {
        return new SecurityMiddlewareRegistration(name, middleware, newOrder, enabled, condition);
    }

    public SecurityMiddlewareRegistration withMiddleware(SecurityMiddleware newMiddleware) {
        return new SecurityMiddlewareRegistration(name, newMiddleware, order, enabled, condition);
    }

    public SecurityMiddlewareRegistration withCondition(Predicate<SecurityRequestContext> newCondition) {
        return new SecurityMiddlewareRegistration(name, middleware, order, enabled, newCondition);
    }
}
