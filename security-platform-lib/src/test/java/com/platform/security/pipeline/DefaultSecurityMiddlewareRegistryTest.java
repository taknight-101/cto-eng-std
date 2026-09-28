package com.platform.security.pipeline;

import com.platform.security.api.SecurityMiddleware;
import com.platform.security.api.SecurityMiddlewareOutcome;
import com.platform.security.api.SecurityMiddlewareRegistration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultSecurityMiddlewareRegistryTest {

    private final DefaultSecurityMiddlewareRegistry registry = new DefaultSecurityMiddlewareRegistry();
    private final SecurityMiddleware noop = (context, chain) -> chain.proceed(context);

    @Test
    void registerAppendsInOrder() {
        registry.register("a", noop);
        registry.register("b", noop);
        registry.register("c", noop);

        assertThat(names()).containsExactly("a", "b", "c");
        assertThat(registry.version()).isEqualTo(3);
    }

    @Test
    void registerBeforeAndAfterInsertRelativeToAnchor() {
        registry.register("middle", noop);
        registry.registerBefore("middle", "first", noop);
        registry.registerAfter("middle", "last", noop);

        assertThat(names()).containsExactly("first", "middle", "last");
    }

    @Test
    void replaceKeepsPositionAndSwapsMiddleware() {
        registry.register("a", noop);
        SecurityMiddleware replacement = (context, chain) -> SecurityMiddlewareOutcome.proceed(context);
        registry.replace("a", replacement);

        assertThat(registry.registrations()).hasSize(1);
        assertThat(registry.registrations().get(0).middleware()).isSameAs(replacement);
    }

    @Test
    void removeDropsRegistration() {
        registry.register("a", noop);
        registry.register("b", noop);
        registry.remove("a");

        assertThat(names()).containsExactly("b");
    }

    @Test
    void disableMarksRegistrationDisabled() {
        registry.register("a", noop);
        registry.disable("a");

        assertThat(registry.registrations().get(0).enabled()).isFalse();
        registry.enable("a");
        assertThat(registry.registrations().get(0).enabled()).isTrue();
    }

    @Test
    void registeringDuplicateNameThrows() {
        registry.register("a", noop);
        assertThatThrownBy(() -> registry.register("a", noop)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void registerBeforeMissingAnchorThrows() {
        assertThatThrownBy(() -> registry.registerBefore("missing", "a", noop))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    private List<String> names() {
        return registry.registrations().stream().map(SecurityMiddlewareRegistration::name).toList();
    }
}
