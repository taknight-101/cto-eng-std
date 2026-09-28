package com.platform.security.pipeline;

import com.platform.security.api.SecurityMiddleware;
import com.platform.security.api.SecurityMiddlewareOutcome;
import com.platform.security.api.SecurityRequestContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSecurityPipelineTest {

    @Test
    void executesMiddlewareInRegisteredOrder() {
        DefaultSecurityMiddlewareRegistry registry = new DefaultSecurityMiddlewareRegistry();
        List<String> order = new ArrayList<>();
        registry.register("first", trackingMiddleware(order, "first"));
        registry.register("second", trackingMiddleware(order, "second"));

        DefaultSecurityPipeline pipeline = new DefaultSecurityPipeline(registry);
        SecurityRequestContext initial = SecurityRequestContext.builder().build();
        SecurityMiddlewareOutcome outcome = pipeline.execute(initial);

        assertThat(order).containsExactly("first", "second");
        assertThat(outcome).isInstanceOf(SecurityMiddlewareOutcome.Continue.class);
    }

    @Test
    void middlewareCanShortCircuit() {
        DefaultSecurityMiddlewareRegistry registry = new DefaultSecurityMiddlewareRegistry();
        registry.register("blocker", (context, chain) -> SecurityMiddlewareOutcome.reject(context, 403, "blocked"));
        registry.register("neverReached", (context, chain) -> {
            throw new AssertionError("should not run");
        });

        DefaultSecurityPipeline pipeline = new DefaultSecurityPipeline(registry);
        SecurityMiddlewareOutcome outcome = pipeline.execute(SecurityRequestContext.builder().build());

        assertThat(outcome).isInstanceOfSatisfying(SecurityMiddlewareOutcome.ShortCircuit.class, sc -> {
            assertThat(sc.status()).isEqualTo(403);
            assertThat(sc.reason()).isEqualTo("blocked");
        });
    }

    @Test
    void unhandledMiddlewareExceptionBecomesFiveHundredShortCircuit() {
        DefaultSecurityMiddlewareRegistry registry = new DefaultSecurityMiddlewareRegistry();
        registry.register("faulty", (context, chain) -> {
            throw new IllegalStateException("kaboom");
        });

        DefaultSecurityPipeline pipeline = new DefaultSecurityPipeline(registry);
        SecurityMiddlewareOutcome outcome = pipeline.execute(SecurityRequestContext.builder().build());

        assertThat(outcome).isInstanceOfSatisfying(SecurityMiddlewareOutcome.ShortCircuit.class,
                sc -> assertThat(sc.status()).isEqualTo(500));
    }

    @Test
    void customOnErrorCanRecoverInsteadOfFailing() {
        DefaultSecurityMiddlewareRegistry registry = new DefaultSecurityMiddlewareRegistry();
        SecurityMiddleware recovering = new SecurityMiddleware() {
            @Override
            public SecurityMiddlewareOutcome handle(SecurityRequestContext context,
                    com.platform.security.api.SecurityMiddlewareChain chain) {
                throw new RuntimeException("expected failure");
            }

            @Override
            public SecurityMiddlewareOutcome onError(SecurityRequestContext context, Throwable error,
                    com.platform.security.api.SecurityMiddlewareChain chain) {
                return SecurityMiddlewareOutcome.reject(context, 418, "recovered");
            }
        };
        registry.register("recovering", recovering);

        DefaultSecurityPipeline pipeline = new DefaultSecurityPipeline(registry);
        SecurityMiddlewareOutcome outcome = pipeline.execute(SecurityRequestContext.builder().build());

        assertThat(outcome).isInstanceOfSatisfying(SecurityMiddlewareOutcome.ShortCircuit.class,
                sc -> assertThat(sc.status()).isEqualTo(418));
    }

    @Test
    void pipelineRecompilesAfterRegistryChanges() {
        DefaultSecurityMiddlewareRegistry registry = new DefaultSecurityMiddlewareRegistry();
        List<String> order = new ArrayList<>();
        registry.register("first", trackingMiddleware(order, "first"));

        DefaultSecurityPipeline pipeline = new DefaultSecurityPipeline(registry);
        pipeline.execute(SecurityRequestContext.builder().build());
        assertThat(order).containsExactly("first");

        registry.register("second", trackingMiddleware(order, "second"));
        pipeline.execute(SecurityRequestContext.builder().build());
        assertThat(order).containsExactly("first", "first", "second");
    }

    private static SecurityMiddleware trackingMiddleware(List<String> order, String name) {
        return (context, chain) -> {
            order.add(name);
            return chain.proceed(context);
        };
    }
}
