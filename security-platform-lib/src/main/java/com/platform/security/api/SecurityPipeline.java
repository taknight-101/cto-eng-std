package com.platform.security.api;

/**
 * The compiled, executable chain of currently-enabled {@link SecurityMiddleware}, built from a
 * {@link SecurityMiddlewareRegistry}.
 *
 * <p>{@link #execute} always returns a result rather than throwing for expected security
 * failures: unexpected exceptions from middleware are caught internally, routed through {@link
 * SecurityMiddleware#onError}, and if still unhandled are surfaced as a {@link
 * SecurityMiddlewareOutcome.ShortCircuit} with status 500 so a single misbehaving middleware
 * cannot crash the servlet filter chain.
 */
public interface SecurityPipeline {

    SecurityMiddlewareOutcome execute(SecurityRequestContext initialContext);
}
