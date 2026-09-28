package com.platform.security.spi;

import com.platform.security.api.SecurityRequestContext;

/**
 * User-implementable extension point letting applications control exactly what response body
 * is written when the pipeline short-circuits (401/403/500), instead of the platform default.
 */
@FunctionalInterface
public interface SecurityFailureHandler {

    SecurityFailureResponse handle(SecurityRequestContext context, int status, String reason);
}
