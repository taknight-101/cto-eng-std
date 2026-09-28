package com.platform.security.api;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Ergonomic way to build a one-off {@link SecurityMiddleware} from lambdas without
 * implementing the interface by hand, e.g.:
 *
 * <pre>{@code
 * registry.register(SecurityMiddleware.named("audit")
 *         .before(ctx -> ctx.withAttribute("audit.start", Instant.now()))
 *         .around((ctx, chain) -> chain.proceed(ctx)));
 * }</pre>
 */
public final class SecurityMiddlewareBuilder {

    private final String name;
    private UnaryOperator<SecurityRequestContext> beforeHook = UnaryOperator.identity();
    private UnaryOperator<SecurityRequestContext> afterHook = UnaryOperator.identity();
    private SecurityMiddlewareErrorHandler errorHandler;

    SecurityMiddlewareBuilder(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /** Runs before the rest of the chain; may transform the context (e.g. add attributes). */
    public SecurityMiddlewareBuilder before(UnaryOperator<SecurityRequestContext> hook) {
        this.beforeHook = Objects.requireNonNull(hook, "hook must not be null");
        return this;
    }

    /** Runs after the rest of the chain completes successfully. */
    public SecurityMiddlewareBuilder after(UnaryOperator<SecurityRequestContext> hook) {
        this.afterHook = Objects.requireNonNull(hook, "hook must not be null");
        return this;
    }

    public SecurityMiddlewareBuilder onError(SecurityMiddlewareErrorHandler handler) {
        this.errorHandler = handler;
        return this;
    }

    /** Terminal operation: supplies full control over whether/how the chain proceeds. */
    public SecurityMiddleware around(SecurityMiddleware handler) {
        Objects.requireNonNull(handler, "handler must not be null");
        return build(handler);
    }

    /** Terminal operation: builds a middleware that always proceeds, using only before/after hooks. */
    public SecurityMiddleware build() {
        return build((ctx, chain) -> chain.proceed(ctx));
    }

    private SecurityMiddleware build(SecurityMiddleware core) {
        UnaryOperator<SecurityRequestContext> before = this.beforeHook;
        UnaryOperator<SecurityRequestContext> after = this.afterHook;
        SecurityMiddlewareErrorHandler onErrorHandler = this.errorHandler;
        return new SecurityMiddleware() {
            @Override
            public SecurityMiddlewareOutcome handle(SecurityRequestContext context, SecurityMiddlewareChain chain)
                    throws Exception {
                SecurityRequestContext beforeContext = before.apply(context);
                SecurityMiddlewareOutcome outcome = core.handle(beforeContext, chain);
                if (outcome instanceof SecurityMiddlewareOutcome.Continue) {
                    return SecurityMiddlewareOutcome.proceed(after.apply(outcome.context()));
                }
                return outcome;
            }

            @Override
            public SecurityMiddlewareOutcome onError(
                    SecurityRequestContext context, Throwable error, SecurityMiddlewareChain chain) throws Exception {
                if (onErrorHandler != null) {
                    return onErrorHandler.handle(context, error, chain);
                }
                return SecurityMiddleware.super.onError(context, error, chain);
            }

            @Override
            public String name() {
                return name;
            }
        };
    }
}
