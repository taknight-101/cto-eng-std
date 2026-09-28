package com.platform.security.api;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable, library-owned view of a single request's security state.
 *
 * <p>Instances never expose {@code HttpServletRequest}/{@code HttpServletResponse} or Spring
 * Security's {@code Authentication} directly as their primary shape; see
 * {@code com.platform.security.spring.ServletRequestAccess} for the explicit escape hatch to
 * the underlying servlet request when it is genuinely needed.
 *
 * <p>Every lifecycle transition ({@link #withPhase}, {@link #withPrincipal}, {@link
 * #withFailure}, {@link #withAttribute}) returns a new instance; a context is a plain
 * immutable value passed explicitly through the pipeline, never mutated in place and never
 * stored in a shared mutable field.
 */
public interface SecurityRequestContext {

    static Builder builder() {
        return new com.platform.security.context.DefaultSecurityRequestContext.Builder();
    }

    String requestId();

    String correlationId();

    Instant timestamp();

    SecurityLifecyclePhase phase();

    Optional<SecurityPrincipal> principal();

    Set<String> authorities();

    boolean authenticated();

    String requestMethod();

    String requestPath();

    Map<String, String> headers();

    Optional<String> clientAddress();

    Map<String, Object> attributes();

    Optional<Throwable> failureCause();

    SecurityRequestContext withPhase(SecurityLifecyclePhase phase);

    SecurityRequestContext withPrincipal(SecurityPrincipal principal, Set<String> authorities);

    SecurityRequestContext withFailure(SecurityLifecyclePhase failurePhase, Throwable cause);

    SecurityRequestContext withAttribute(String key, Object value);

    /** Builder for the initial context created at the start of the pipeline. */
    interface Builder {
        Builder requestId(String requestId);

        Builder correlationId(String correlationId);

        Builder requestMethod(String method);

        Builder requestPath(String path);

        Builder header(String name, String value);

        Builder headers(Map<String, String> headers);

        Builder clientAddress(String clientAddress);

        Builder attribute(String key, Object value);

        SecurityRequestContext build();
    }
}
