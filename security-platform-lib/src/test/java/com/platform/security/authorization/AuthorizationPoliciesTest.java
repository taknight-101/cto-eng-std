package com.platform.security.authorization;

import com.platform.security.api.SecurityRequestContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationPoliciesTest {

    private static SecurityRequestContext contextWithAuthorities(String... authorities) {
        SecurityRequestContext ctx = SecurityRequestContext.builder().build();
        return ctx.withPrincipal(com.platform.security.api.SecurityPrincipal.of("user"), Set.of(authorities));
    }

    @Test
    void requireAuthorityAllowsWhenPresent() {
        var policy = AuthorizationPolicies.requireAuthority("ROLE_ADMIN");
        assertThat(policy.evaluate(contextWithAuthorities("ROLE_ADMIN")).allowed()).isTrue();
        assertThat(policy.evaluate(contextWithAuthorities("ROLE_USER")).allowed()).isFalse();
    }

    @Test
    void andShortCircuitsOnFirstDenial() {
        var policy = AuthorizationPolicies.requireAuthority("ROLE_A").and(AuthorizationPolicies.requireAuthority("ROLE_B"));
        assertThat(policy.evaluate(contextWithAuthorities("ROLE_A")).allowed()).isFalse();
        assertThat(policy.evaluate(contextWithAuthorities("ROLE_A", "ROLE_B")).allowed()).isTrue();
    }

    @Test
    void orAllowsIfEitherAllows() {
        var policy = AuthorizationPolicies.requireAuthority("ROLE_A").or(AuthorizationPolicies.requireAuthority("ROLE_B"));
        assertThat(policy.evaluate(contextWithAuthorities("ROLE_B")).allowed()).isTrue();
        assertThat(policy.evaluate(contextWithAuthorities("ROLE_C")).allowed()).isFalse();
    }

    @Test
    void negateInvertsDecision() {
        var policy = AuthorizationPolicies.requireAuthority("ROLE_A").negate();
        assertThat(policy.evaluate(contextWithAuthorities("ROLE_A")).allowed()).isFalse();
        assertThat(policy.evaluate(contextWithAuthorities()).allowed()).isTrue();
    }

    @Test
    void permitAllAlwaysAllows() {
        assertThat(AuthorizationPolicies.permitAll().evaluate(contextWithAuthorities()).allowed()).isTrue();
    }
}
