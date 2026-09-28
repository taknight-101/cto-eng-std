package com.platform.security.api;

/** Error-handling hook usable with {@link SecurityMiddlewareBuilder#onError}. */
@FunctionalInterface
public interface SecurityMiddlewareErrorHandler {

    SecurityMiddlewareOutcome handle(
            SecurityRequestContext context, Throwable error, SecurityMiddlewareChain chain) throws Exception;
}
