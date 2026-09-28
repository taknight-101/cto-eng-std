package com.platform.security.auth;

import com.platform.security.api.AuthenticationFailedException;
import com.platform.security.api.AuthenticationOutcome;
import com.platform.security.api.SecurityErrorCode;
import com.platform.security.api.SecurityEvent;
import com.platform.security.api.SecurityEventDrill;
import com.platform.security.api.SecurityEventType;
import com.platform.security.api.SecurityLifecyclePhase;
import com.platform.security.api.SecurityMiddleware;
import com.platform.security.api.SecurityMiddlewareChain;
import com.platform.security.api.SecurityMiddlewareOutcome;
import com.platform.security.api.SecurityPipelineStages;
import com.platform.security.api.SecurityPrincipal;
import com.platform.security.api.SecurityRequestContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Built-in middleware, registered by auto-configuration at {@code
 * SecurityPipelineStages.AUTHENTICATION}, that tries each configured {@link
 * com.platform.security.api.TokenAuthenticator} in order until one produces a definitive
 * result, installing the resulting {@code Authentication} into Spring's own {@code
 * SecurityContextHolder}.
 */
public final class AuthenticationMiddleware implements SecurityMiddleware {

    private final List<com.platform.security.api.TokenAuthenticator> authenticators;
    private final SecurityEventDrill eventDrill;
    private final boolean authenticationRequired;

    public AuthenticationMiddleware(
            List<com.platform.security.api.TokenAuthenticator> authenticators,
            SecurityEventDrill eventDrill,
            boolean authenticationRequired) {
        this.authenticators = List.copyOf(authenticators);
        this.eventDrill = eventDrill;
        this.authenticationRequired = authenticationRequired;
    }

    @Override
    public SecurityMiddlewareOutcome handle(SecurityRequestContext context, SecurityMiddlewareChain chain)
            throws Exception {
        SecurityRequestContext authenticating = context.withPhase(SecurityLifecyclePhase.AUTHENTICATING);
        eventDrill.publish(SecurityEvent.of(
                SecurityEventType.AUTHENTICATION_STARTED, authenticating, "authentication", Map.of()));

        for (com.platform.security.api.TokenAuthenticator authenticator : authenticators) {
            AuthenticationOutcome outcome = authenticator.authenticate(authenticating);
            if (outcome instanceof AuthenticationOutcome.Success success) {
                return onSuccess(authenticating, success.authentication(), chain);
            }
            if (outcome instanceof AuthenticationOutcome.Failure failure) {
                return onFailure(authenticating, failure.errorCode(), failure.reason(), failure.cause());
            }
            // NotApplicable -> try the next authenticator.
        }

        if (authenticationRequired) {
            return onFailure(authenticating, SecurityErrorCode.MISSING_CREDENTIALS,
                    "No applicable credentials found", null);
        }
        return chain.proceed(authenticating);
    }

    private SecurityMiddlewareOutcome onSuccess(
            SecurityRequestContext authenticating, Authentication authentication, SecurityMiddlewareChain chain)
            throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        SecurityPrincipal principal = SecurityPrincipal.of(String.valueOf(authentication.getPrincipal()));
        SecurityRequestContext authenticated = authenticating.withPrincipal(principal, authorities)
                .withPhase(SecurityLifecyclePhase.AUTHENTICATED);
        eventDrill.publish(SecurityEvent.of(
                SecurityEventType.AUTHENTICATION_SUCCEEDED, authenticated, "authentication", Map.of()));
        return chain.proceed(authenticated);
    }

    private SecurityMiddlewareOutcome onFailure(
            SecurityRequestContext authenticating, SecurityErrorCode errorCode, String reason, Throwable cause) {
        SecurityRequestContext failed = authenticating.withFailure(
                SecurityLifecyclePhase.AUTHENTICATION_FAILED,
                new AuthenticationFailedException(reason, cause, errorCode, Map.of()));
        SecurityEventType type = switch (errorCode) {
            case TOKEN_EXPIRED -> SecurityEventType.TOKEN_EXPIRED;
            case INVALID_TOKEN -> SecurityEventType.INVALID_TOKEN;
            case MISSING_CREDENTIALS -> SecurityEventType.MISSING_CREDENTIALS;
            default -> SecurityEventType.AUTHENTICATION_FAILED;
        };
        eventDrill.publish(SecurityEvent.of(type, failed, cause, "authentication", Map.of("reason", reason)));
        return SecurityMiddlewareOutcome.reject(failed, 401, reason);
    }

    @Override
    public String name() {
        return SecurityPipelineStages.AUTHENTICATION;
    }
}
