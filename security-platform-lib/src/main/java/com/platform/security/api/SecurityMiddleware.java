package com.platform.security.api;

/**
 * A single step in the {@link SecurityPipeline}.
 *
 * <p>Deliberately a single composable method rather than separate {@code before}/{@code
 * around}/{@code after} registrations: a middleware that only needs "before" behavior simply
 * calls {@code chain.proceed(context)} immediately and returns its result; one that needs
 * "after" behavior calls {@code chain.proceed(context)} first and then acts on the outcome.
 * This keeps the mental model and the registry API small while still supporting every case
 * the four-hook design would have.
 */
@FunctionalInterface
public interface SecurityMiddleware {

    SecurityMiddlewareOutcome handle(SecurityRequestContext context, SecurityMiddlewareChain chain) throws Exception;

    /**
     * Invoked when {@link #handle} (or a downstream middleware) throws. The default
     * behavior rethrows so the pipeline records a {@link SecurityMiddlewareException}; override
     * to recover (e.g. to short-circuit with a specific status) instead of failing the request.
     */
    default SecurityMiddlewareOutcome onError(
            SecurityRequestContext context, Throwable error, SecurityMiddlewareChain chain) throws Exception {
        if (error instanceof RuntimeException re) {
            throw re;
        }
        throw new RuntimeException(error);
    }

    /** Display name used in events/diagnostics when none is supplied at registration time. */
    default String name() {
        return getClass().getSimpleName();
    }

    static SecurityMiddlewareBuilder named(String name) {
        return new SecurityMiddlewareBuilder(name);
    }
}
