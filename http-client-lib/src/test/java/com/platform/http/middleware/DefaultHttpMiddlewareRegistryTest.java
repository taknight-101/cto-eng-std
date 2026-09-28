package com.platform.http.middleware;

import com.platform.http.api.HttpRequestSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultHttpMiddlewareRegistryTest {

    private final DefaultHttpMiddlewareRegistry registry = new DefaultHttpMiddlewareRegistry();
    private final HttpMiddleware noop = (request, chain) -> chain.proceed(request);

    @Test
    void registerAppendsInOrder() {
        registry.register("a", noop);
        registry.register("b", noop);
        assertThat(names()).containsExactly("a", "b");
    }

    @Test
    void registerBeforeAndAfterInsertRelativeToAnchor() {
        registry.register("middle", noop);
        registry.registerBefore("middle", "first", noop);
        registry.registerAfter("middle", "last", noop);
        assertThat(names()).containsExactly("first", "middle", "last");
    }

    @Test
    void removeAndDisableWork() {
        registry.register("a", noop);
        registry.disable("a");
        assertThat(registry.registrations().get(0).enabled()).isFalse();
        registry.remove("a");
        assertThat(registry.registrations()).isEmpty();
    }

    @Test
    void conditionIsAppliedToRegistration() {
        registry.register("a", noop);
        registry.condition("a", (HttpRequestSpec r) -> r.uri().startsWith("/api"));
        assertThat(registry.registrations().get(0).condition().test(HttpRequestSpec.get("/api/x").build())).isTrue();
        assertThat(registry.registrations().get(0).condition().test(HttpRequestSpec.get("/other").build())).isFalse();
    }

    @Test
    void duplicateNameThrows() {
        registry.register("a", noop);
        assertThatThrownBy(() -> registry.register("a", noop)).isInstanceOf(IllegalStateException.class);
    }

    private List<String> names() {
        return registry.registrations().stream().map(HttpMiddlewareRegistration::name).toList();
    }
}
