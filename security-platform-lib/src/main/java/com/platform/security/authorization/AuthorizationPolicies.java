package com.platform.security.authorization;

import com.platform.security.api.AuthorizationDecision;
import com.platform.security.api.AuthorizationPolicy;
import com.platform.security.api.SecurityRequestContext;

import java.util.Arrays;
import java.util.function.Predicate;

/** Convenience factory for common {@link AuthorizationPolicy} building blocks. */
public final class AuthorizationPolicies {

    private AuthorizationPolicies() {
    }

    public static AuthorizationPolicy requireAuthority(String authority) {
        return context -> context.authorities().contains(authority)
                ? AuthorizationDecision.allow()
                : AuthorizationDecision.deny("Missing required authority: " + authority);
    }

    public static AuthorizationPolicy requireAnyAuthority(String... authorities) {
        return context -> Arrays.stream(authorities).anyMatch(context.authorities()::contains)
                ? AuthorizationDecision.allow()
                : AuthorizationDecision.deny("Missing any of required authorities: " + Arrays.toString(authorities));
    }

    public static AuthorizationPolicy requireAllAuthorities(String... authorities) {
        return context -> Arrays.stream(authorities).allMatch(context.authorities()::contains)
                ? AuthorizationDecision.allow()
                : AuthorizationDecision.deny("Missing one of required authorities: " + Arrays.toString(authorities));
    }

    public static AuthorizationPolicy allowIf(Predicate<SecurityRequestContext> predicate, String denyReason) {
        return context -> predicate.test(context) ? AuthorizationDecision.allow() : AuthorizationDecision.deny(denyReason);
    }

    public static AuthorizationPolicy permitAll() {
        return context -> AuthorizationDecision.allow();
    }
}
