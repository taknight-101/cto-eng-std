package com.platform.security.api;

import java.util.List;
import java.util.function.Predicate;

/**
 * Mutable configuration store for {@link SecurityMiddleware} registrations. Distinct from
 * {@link SecurityPipeline}: the registry is the configuration surface (what should run, in
 * what order), the pipeline is the compiled, executable chain built from the registry's
 * current state.
 *
 * <p>Implementations must be thread-safe: registration changes made from application startup
 * code (or dynamically at runtime) must be safely visible to concurrent request-handling
 * threads executing the pipeline.
 */
public interface SecurityMiddlewareRegistry {

    SecurityMiddlewareRegistry register(SecurityMiddleware middleware);

    SecurityMiddlewareRegistry register(String name, SecurityMiddleware middleware);

    SecurityMiddlewareRegistry register(String name, SecurityMiddleware middleware, int order);

    SecurityMiddlewareRegistry registerBefore(String existingName, String name, SecurityMiddleware middleware);

    SecurityMiddlewareRegistry registerAfter(String existingName, String name, SecurityMiddleware middleware);

    SecurityMiddlewareRegistry replace(String name, SecurityMiddleware middleware);

    SecurityMiddlewareRegistry remove(String name);

    SecurityMiddlewareRegistry enable(String name);

    SecurityMiddlewareRegistry disable(String name);

    SecurityMiddlewareRegistry condition(String name, Predicate<SecurityRequestContext> condition);

    /** Ordered, immutable snapshot of the current registrations (enabled and disabled). */
    List<SecurityMiddlewareRegistration> registrations();

    /** Monotonically increasing counter bumped on every mutation; used by the pipeline to detect staleness. */
    long version();
}
