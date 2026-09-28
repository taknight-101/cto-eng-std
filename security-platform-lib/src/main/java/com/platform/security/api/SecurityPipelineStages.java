package com.platform.security.api;

/**
 * Well-known {@link SecurityMiddlewareRegistration} names and default orders used by the
 * built-in authentication and authorization middleware, so user middleware can be positioned
 * relative to them with {@code registerBefore}/{@code registerAfter} instead of needing raw
 * numeric order values.
 */
public final class SecurityPipelineStages {

    public static final String AUTHENTICATION = "authentication";
    public static final int AUTHENTICATION_ORDER = 1000;

    public static final String AUTHORIZATION = "authorization";
    public static final int AUTHORIZATION_ORDER = 2000;

    private SecurityPipelineStages() {
    }
}
