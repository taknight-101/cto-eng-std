package com.platform.security.spring;

import com.platform.security.spi.SecurityFailureHandler;
import com.platform.security.spi.SecurityFailureResponse;

/** Default {@link SecurityFailureHandler}: a small JSON body with status and reason, no stack traces or internals. */
public final class DefaultSecurityFailureHandler implements SecurityFailureHandler {

    @Override
    public SecurityFailureResponse handle(
            com.platform.security.api.SecurityRequestContext context, int status, String reason) {
        return new SecurityFailureResponse(
                status,
                java.util.Map.of(
                        "status", status,
                        "error", reason == null ? "Security processing rejected the request" : reason,
                        "correlationId", context.correlationId()),
                java.util.Map.of());
    }
}
