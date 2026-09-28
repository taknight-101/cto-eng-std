package com.platform.security.spi;

import com.platform.security.api.SecurityRequestContext;

/**
 * User-implementable extension point for enriching the context after authentication (e.g.
 * loading a user profile, tenant, or feature flags into {@code attributes()}).
 */
@FunctionalInterface
public interface SecurityContextEnricher {

    SecurityRequestContext enrich(SecurityRequestContext context);
}
