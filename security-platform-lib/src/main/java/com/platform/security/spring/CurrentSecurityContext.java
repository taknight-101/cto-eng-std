package com.platform.security.spring;

import com.platform.security.api.SecurityRequestContext;

import java.util.Optional;

/**
 * Escape hatch to the current request's {@link SecurityRequestContext} from application code
 * (e.g. to propagate {@code correlationId()} into an outbound {@code HttpRequestSpec}), without
 * exposing the servlet request attribute key or requiring a bean injection at every call site.
 * Only populated for requests that passed through {@link SecurityMiddlewareFilter}.
 */
public final class CurrentSecurityContext {

    private CurrentSecurityContext() {
    }

    public static Optional<SecurityRequestContext> current() {
        return ServletRequestAccess.currentRequest()
                .map(request -> request.getAttribute(SecurityMiddlewareFilter.CONTEXT_REQUEST_ATTRIBUTE))
                .filter(SecurityRequestContext.class::isInstance)
                .map(SecurityRequestContext.class::cast);
    }
}
