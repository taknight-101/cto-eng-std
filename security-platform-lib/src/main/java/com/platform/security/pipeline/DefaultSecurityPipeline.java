package com.platform.security.pipeline;

import com.platform.security.api.SecurityLifecyclePhase;
import com.platform.security.api.SecurityMiddlewareChain;
import com.platform.security.api.SecurityMiddlewareException;
import com.platform.security.api.SecurityMiddlewareOutcome;
import com.platform.security.api.SecurityMiddlewareRegistration;
import com.platform.security.api.SecurityMiddlewareRegistry;
import com.platform.security.api.SecurityPipeline;
import com.platform.security.api.SecurityRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Compiles the currently-enabled {@link SecurityMiddlewareRegistration}s from a {@link
 * SecurityMiddlewareRegistry} into an executable chain, recompiling only when {@link
 * SecurityMiddlewareRegistry#version()} changes (registrations mutate rarely; requests are
 * frequent).
 */
public final class DefaultSecurityPipeline implements SecurityPipeline {

    private static final Logger log = LoggerFactory.getLogger(DefaultSecurityPipeline.class);

    private final SecurityMiddlewareRegistry registry;
    private final ReentrantLock compileLock = new ReentrantLock();
    private volatile long compiledVersion = -1;
    private volatile SecurityMiddlewareChain compiledChain;

    public DefaultSecurityPipeline(SecurityMiddlewareRegistry registry) {
        this.registry = registry;
    }

    @Override
    public SecurityMiddlewareOutcome execute(SecurityRequestContext initialContext) {
        try {
            return compiledChain().proceed(initialContext);
        } catch (Exception e) {
            log.error("Unhandled exception escaped the security pipeline for request {}",
                    initialContext.requestId(), e);
            SecurityRequestContext failed = initialContext.withFailure(SecurityLifecyclePhase.FAILED, e);
            return SecurityMiddlewareOutcome.reject(failed, 500, "Unexpected security pipeline failure");
        }
    }

    private SecurityMiddlewareChain compiledChain() {
        long currentVersion = registry.version();
        SecurityMiddlewareChain cached = compiledChain;
        if (cached != null && compiledVersion == currentVersion) {
            return cached;
        }
        compileLock.lock();
        try {
            currentVersion = registry.version();
            cached = compiledChain;
            if (cached != null && compiledVersion == currentVersion) {
                return cached;
            }
            SecurityMiddlewareChain compiled = compile(registry.registrations());
            compiledChain = compiled;
            compiledVersion = currentVersion;
            return compiled;
        } finally {
            compileLock.unlock();
        }
    }

    private static SecurityMiddlewareChain compile(List<SecurityMiddlewareRegistration> registrations) {
        SecurityMiddlewareChain chain = SecurityMiddlewareOutcome::proceed;
        for (int i = registrations.size() - 1; i >= 0; i--) {
            SecurityMiddlewareRegistration registration = registrations.get(i);
            SecurityMiddlewareChain next = chain;
            chain = context -> {
                if (!registration.enabled() || !registration.condition().test(context)) {
                    return next.proceed(context);
                }
                try {
                    return registration.middleware().handle(context, next);
                } catch (Exception ex) {
                    try {
                        return registration.middleware().onError(context, ex, next);
                    } catch (Exception fatal) {
                        throw new SecurityMiddlewareException(
                                "Middleware '" + registration.name() + "' failed", fatal, registration.name());
                    }
                }
            };
        }
        return chain;
    }
}
