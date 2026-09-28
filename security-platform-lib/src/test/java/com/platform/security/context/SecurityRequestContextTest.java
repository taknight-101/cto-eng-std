package com.platform.security.context;

import com.platform.security.api.SecurityLifecyclePhase;
import com.platform.security.api.SecurityPrincipal;
import com.platform.security.api.SecurityRequestContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityRequestContextTest {

    @Test
    void builderProducesReceivedPhaseWithDefaults() {
        SecurityRequestContext context = SecurityRequestContext.builder()
                .requestMethod("GET")
                .requestPath("/api/users/1")
                .header("Authorization", "Bearer abc")
                .build();

        assertThat(context.phase()).isEqualTo(SecurityLifecyclePhase.RECEIVED);
        assertThat(context.authenticated()).isFalse();
        assertThat(context.principal()).isEmpty();
        assertThat(context.requestMethod()).isEqualTo("GET");
        assertThat(context.headers()).containsEntry("Authorization", "Bearer abc");
        assertThat(context.correlationId()).isNotBlank();
        assertThat(context.requestId()).isNotBlank();
    }

    @Test
    void withPhaseReturnsNewImmutableInstance() {
        SecurityRequestContext original = SecurityRequestContext.builder().build();
        SecurityRequestContext transitioned = original.withPhase(SecurityLifecyclePhase.AUTHENTICATING);

        assertThat(original.phase()).isEqualTo(SecurityLifecyclePhase.RECEIVED);
        assertThat(transitioned.phase()).isEqualTo(SecurityLifecyclePhase.AUTHENTICATING);
        assertThat(transitioned).isNotSameAs(original);
    }

    @Test
    void withPrincipalMarksAuthenticatedWhenPhaseIsNotFailure() {
        SecurityRequestContext context = SecurityRequestContext.builder().build()
                .withPhase(SecurityLifecyclePhase.AUTHENTICATED)
                .withPrincipal(SecurityPrincipal.of("alice"), Set.of("ROLE_USER"));

        assertThat(context.authenticated()).isTrue();
        assertThat(context.principal()).contains(SecurityPrincipal.of("alice"));
        assertThat(context.authorities()).containsExactly("ROLE_USER");
    }

    @Test
    void withFailureRecordsCauseAndFailurePhase() {
        RuntimeException cause = new RuntimeException("boom");
        SecurityRequestContext context = SecurityRequestContext.builder().build()
                .withFailure(SecurityLifecyclePhase.AUTHENTICATION_FAILED, cause);

        assertThat(context.phase()).isEqualTo(SecurityLifecyclePhase.AUTHENTICATION_FAILED);
        assertThat(context.failureCause()).contains(cause);
        assertThat(context.phase().isFailure()).isTrue();
    }

    @Test
    void withAttributeIsAdditiveAndImmutable() {
        SecurityRequestContext original = SecurityRequestContext.builder().attribute("a", 1).build();
        SecurityRequestContext updated = original.withAttribute("b", 2);

        assertThat(original.attributes()).containsOnly(org.assertj.core.api.Assertions.entry("a", 1));
        assertThat(updated.attributes()).containsOnly(
                org.assertj.core.api.Assertions.entry("a", 1), org.assertj.core.api.Assertions.entry("b", 2));
    }
}
