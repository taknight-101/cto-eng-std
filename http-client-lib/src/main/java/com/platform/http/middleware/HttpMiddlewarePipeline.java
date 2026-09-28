package com.platform.http.middleware;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Compiles the currently-enabled {@link HttpMiddlewareRegistration}s from a {@link
 * HttpMiddlewareRegistry} into an executable chain terminating in the fixed downstream
 * dispatch step, recompiling only when {@link HttpMiddlewareRegistry#version()} changes.
 */
public final class HttpMiddlewarePipeline {

    private final HttpMiddlewareRegistry registry;
    private final HttpMiddlewareChain terminal;
    private final ReentrantLock compileLock = new ReentrantLock();
    private volatile long compiledVersion = -1;
    private volatile HttpMiddlewareChain compiledChain;

    public HttpMiddlewarePipeline(HttpMiddlewareRegistry registry, HttpMiddlewareChain terminal) {
        this.registry = registry;
        this.terminal = terminal;
    }

    public HttpMiddlewareOutcome execute(com.platform.http.api.HttpRequestSpec request) throws Exception {
        return compiledChain().proceed(request);
    }

    private HttpMiddlewareChain compiledChain() {
        long currentVersion = registry.version();
        HttpMiddlewareChain cached = compiledChain;
        if (cached != null && compiledVersion == currentVersion) {
            return cached;
        }
        compileLock.lock();
        try {
            currentVersion = registry.version();
            cached = compiledChain;
            if (cached != null && compiledVersion == currentVersion) {
                return cached;
            }
            HttpMiddlewareChain compiled = compile(registry.registrations(), terminal);
            compiledChain = compiled;
            compiledVersion = currentVersion;
            return compiled;
        } finally {
            compileLock.unlock();
        }
    }

    private static HttpMiddlewareChain compile(List<HttpMiddlewareRegistration> registrations, HttpMiddlewareChain terminal) {
        HttpMiddlewareChain chain = terminal;
        for (int i = registrations.size() - 1; i >= 0; i--) {
            HttpMiddlewareRegistration registration = registrations.get(i);
            HttpMiddlewareChain next = chain;
            chain = request -> {
                if (!registration.enabled() || !registration.condition().test(request)) {
                    return next.proceed(request);
                }
                try {
                    return registration.middleware().handle(request, next);
                } catch (Exception ex) {
                    try {
                        return registration.middleware().onError(request, ex, next);
                    } catch (Exception fatal) {
                        throw new HttpMiddlewareException(
                                "Middleware '" + registration.name() + "' failed", fatal, registration.name());
                    }
                }
            };
        }
        return chain;
    }
}
