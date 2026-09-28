package com.platform.http.middleware;

import com.platform.http.api.HttpRequestSpec;
import java.util.List;
import java.util.function.Predicate;

/** Thread-safe configuration store for {@link HttpMiddleware} registrations. */
public interface HttpMiddlewareRegistry {

    HttpMiddlewareRegistry register(HttpMiddleware middleware);

    HttpMiddlewareRegistry register(String name, HttpMiddleware middleware);

    HttpMiddlewareRegistry register(String name, HttpMiddleware middleware, int order);

    HttpMiddlewareRegistry registerBefore(String existingName, String name, HttpMiddleware middleware);

    HttpMiddlewareRegistry registerAfter(String existingName, String name, HttpMiddleware middleware);

    HttpMiddlewareRegistry replace(String name, HttpMiddleware middleware);

    HttpMiddlewareRegistry remove(String name);

    HttpMiddlewareRegistry enable(String name);

    HttpMiddlewareRegistry disable(String name);

    HttpMiddlewareRegistry condition(String name, Predicate<HttpRequestSpec> condition);

    List<HttpMiddlewareRegistration> registrations();

    long version();
}
