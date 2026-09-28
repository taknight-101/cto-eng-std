package com.platform.security.api;

/**
 * Pluggable authorization rule, evaluated after authentication. Composable via {@link #and},
 * {@link #or} and {@link #negate} so applications can build rich rules without a new
 * abstraction per combination.
 */
@FunctionalInterface
public interface AuthorizationPolicy {

    AuthorizationDecision evaluate(SecurityRequestContext context);

    default AuthorizationPolicy and(AuthorizationPolicy other) {
        return context -> {
            AuthorizationDecision decision = this.evaluate(context);
            return decision.allowed() ? other.evaluate(context) : decision;
        };
    }

    default AuthorizationPolicy or(AuthorizationPolicy other) {
        return context -> {
            AuthorizationDecision decision = this.evaluate(context);
            return decision.allowed() ? decision : other.evaluate(context);
        };
    }

    default AuthorizationPolicy negate() {
        return context -> {
            AuthorizationDecision decision = this.evaluate(context);
            return decision.allowed()
                    ? AuthorizationDecision.deny("negated policy denied access")
                    : AuthorizationDecision.allow();
        };
    }
}
