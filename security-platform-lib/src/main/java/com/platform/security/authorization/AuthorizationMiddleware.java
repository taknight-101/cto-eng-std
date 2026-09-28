package com.platform.security.authorization;

import com.platform.security.api.AuthorizationDecision;
import com.platform.security.api.AuthorizationPolicy;
import com.platform.security.api.SecurityErrorCode;
import com.platform.security.api.SecurityEvent;
import com.platform.security.api.SecurityEventDrill;
import com.platform.security.api.SecurityEventType;
import com.platform.security.api.SecurityLifecyclePhase;
import com.platform.security.api.SecurityMiddleware;
import com.platform.security.api.SecurityMiddlewareChain;
import com.platform.security.api.SecurityMiddlewareOutcome;
import com.platform.security.api.SecurityRequestContext;

import java.util.Map;

/**
 * Built-in middleware, registered by auto-configuration at {@code
 * SecurityPipelineStages.AUTHORIZATION}, that evaluates a single composed {@link
 * AuthorizationPolicy} against the (by then, authenticated-or-not) request context.
 */
public final class AuthorizationMiddleware implements SecurityMiddleware {

    private final AuthorizationPolicy policy;
    private final SecurityEventDrill eventDrill;

    public AuthorizationMiddleware(AuthorizationPolicy policy, SecurityEventDrill eventDrill) {
        this.policy = policy;
        this.eventDrill = eventDrill;
    }

    @Override
    public SecurityMiddlewareOutcome handle(SecurityRequestContext context, SecurityMiddlewareChain chain)
            throws Exception {
        SecurityRequestContext authorizing = context.withPhase(SecurityLifecyclePhase.AUTHORIZING);
        eventDrill.publish(SecurityEvent.of(SecurityEventType.AUTHORIZATION_STARTED, authorizing, "authorization", Map.of()));

        AuthorizationDecision decision = policy.evaluate(authorizing);
        if (decision.allowed()) {
            SecurityRequestContext authorized = authorizing.withPhase(SecurityLifecyclePhase.AUTHORIZED);
            eventDrill.publish(SecurityEvent.of(
                    SecurityEventType.AUTHORIZATION_SUCCEEDED, authorized, "authorization", Map.of()));
            return chain.proceed(authorized);
        }

        String reason = decision instanceof AuthorizationDecision.Deny deny ? deny.reason() : "denied";
        SecurityRequestContext failed = authorizing.withFailure(SecurityLifecyclePhase.AUTHORIZATION_FAILED,
                new com.platform.security.api.AuthorizationDeniedException(reason));
        eventDrill.publish(SecurityEvent.of(SecurityEventType.AUTHORIZATION_FAILED, failed, "authorization",
                Map.of("reason", reason, "errorCode", SecurityErrorCode.AUTHORIZATION_DENIED)));
        eventDrill.publish(SecurityEvent.of(SecurityEventType.ACCESS_DENIED, failed, "authorization", Map.of("reason", reason)));
        return SecurityMiddlewareOutcome.reject(failed, 403, reason);
    }

    @Override
    public String name() {
        return com.platform.security.api.SecurityPipelineStages.AUTHORIZATION;
    }
}
