package com.platform.security.api;

/**
 * Result of a single {@link SecurityMiddleware} step. A richer domain model than a boolean
 * {@code shouldContinue()} so the reason for short-circuiting is observable by the pipeline,
 * events and diagnostics.
 */
public sealed interface SecurityMiddlewareOutcome {

    SecurityRequestContext context();

    static SecurityMiddlewareOutcome proceed(SecurityRequestContext context) {
        return new Continue(context);
    }

    static SecurityMiddlewareOutcome reject(SecurityRequestContext context, int status, String reason) {
        return new ShortCircuit(context, status, reason);
    }

    /** Continue executing the remainder of the pipeline (and, eventually, the request). */
    record Continue(SecurityRequestContext context) implements SecurityMiddlewareOutcome {
    }

    /** Stop pipeline execution and respond immediately with the given status/reason. */
    record ShortCircuit(SecurityRequestContext context, int status, String reason) implements SecurityMiddlewareOutcome {
    }
}
