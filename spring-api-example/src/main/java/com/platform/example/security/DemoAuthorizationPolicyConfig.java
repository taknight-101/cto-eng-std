package com.platform.example.security;

import com.platform.security.api.AuthorizationDecision;
import com.platform.security.api.AuthorizationPolicy;
import com.platform.security.api.SecurityRequestContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Path-based demo policy: {@code /api/orders/**} requires {@code ROLE_ADMIN}, everything else
 * under {@code /api/**} requires {@code ROLE_USER} (which {@code ROLE_ADMIN} also satisfies).
 * A real application would typically compose per-endpoint policies via method security instead
 * of a single global policy, but a single {@link AuthorizationPolicy} bean is what the platform
 * wires into the built-in authorization middleware, so this demonstrates that extension point.
 */
@Configuration
public class DemoAuthorizationPolicyConfig {

    @Bean
    public AuthorizationPolicy authorizationPolicy() {
        return this::evaluate;
    }

    private AuthorizationDecision evaluate(SecurityRequestContext context) {
        String path = context.requestPath();
        if (path.startsWith("/api/orders")) {
            return context.authorities().contains("ROLE_ADMIN")
                    ? AuthorizationDecision.allow()
                    : AuthorizationDecision.deny("ROLE_ADMIN is required to manage orders");
        }
        if (path.startsWith("/api/users") || path.startsWith("/api/gateway")) {
            return (context.authorities().contains("ROLE_USER") || context.authorities().contains("ROLE_ADMIN"))
                    ? AuthorizationDecision.allow()
                    : AuthorizationDecision.deny("ROLE_USER is required");
        }
        return AuthorizationDecision.allow();
    }
}
