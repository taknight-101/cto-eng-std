package com.platform.security.context;

import com.platform.security.api.SecurityLifecyclePhase;
import com.platform.security.api.SecurityPrincipal;
import com.platform.security.api.SecurityRequestContext;
import com.platform.security.internal.CorrelationIds;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Sole implementation of {@link SecurityRequestContext}. Package-private construction is
 * intentional: application and middleware code only ever obtains instances via {@link
 * SecurityRequestContext#builder()} or the {@code withXxx} transition methods.
 */
public record DefaultSecurityRequestContext(
        String requestId,
        String correlationId,
        Instant timestamp,
        SecurityLifecyclePhase phase,
        Optional<SecurityPrincipal> principal,
        Set<String> authorities,
        String requestMethod,
        String requestPath,
        Map<String, String> headers,
        Optional<String> clientAddress,
        Map<String, Object> attributes,
        Optional<Throwable> failureCause) implements SecurityRequestContext {

    public DefaultSecurityRequestContext {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(phase, "phase must not be null");
        principal = principal == null ? Optional.empty() : principal;
        authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
        requestMethod = requestMethod == null ? "" : requestMethod;
        requestPath = requestPath == null ? "" : requestPath;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        clientAddress = clientAddress == null ? Optional.empty() : clientAddress;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        failureCause = failureCause == null ? Optional.empty() : failureCause;
    }

    @Override
    public boolean authenticated() {
        return principal.isPresent() && !phase.isFailure();
    }

    @Override
    public SecurityRequestContext withPhase(SecurityLifecyclePhase newPhase) {
        return new DefaultSecurityRequestContext(requestId, correlationId, timestamp, newPhase, principal,
                authorities, requestMethod, requestPath, headers, clientAddress, attributes, failureCause);
    }

    @Override
    public SecurityRequestContext withPrincipal(SecurityPrincipal newPrincipal, Set<String> newAuthorities) {
        return new DefaultSecurityRequestContext(requestId, correlationId, timestamp, phase,
                Optional.ofNullable(newPrincipal), newAuthorities, requestMethod, requestPath, headers,
                clientAddress, attributes, failureCause);
    }

    @Override
    public SecurityRequestContext withFailure(SecurityLifecyclePhase failurePhase, Throwable cause) {
        return new DefaultSecurityRequestContext(requestId, correlationId, timestamp, failurePhase, principal,
                authorities, requestMethod, requestPath, headers, clientAddress, attributes,
                Optional.ofNullable(cause));
    }

    @Override
    public SecurityRequestContext withAttribute(String key, Object value) {
        Map<String, Object> updated = new LinkedHashMap<>(attributes);
        updated.put(key, value);
        return new DefaultSecurityRequestContext(requestId, correlationId, timestamp, phase, principal, authorities,
                requestMethod, requestPath, headers, clientAddress, updated, failureCause);
    }

    public static final class Builder implements SecurityRequestContext.Builder {
        private String requestId = CorrelationIds.newId();
        private String correlationId = CorrelationIds.newId();
        private String requestMethod = "";
        private String requestPath = "";
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String clientAddress;
        private final Map<String, Object> attributes = new LinkedHashMap<>();

        @Override
        public SecurityRequestContext.Builder requestId(String requestId) {
            this.requestId = Objects.requireNonNull(requestId, "requestId must not be null");
            return this;
        }

        @Override
        public SecurityRequestContext.Builder correlationId(String correlationId) {
            this.correlationId = Objects.requireNonNull(correlationId, "correlationId must not be null");
            return this;
        }

        @Override
        public SecurityRequestContext.Builder requestMethod(String method) {
            this.requestMethod = method;
            return this;
        }

        @Override
        public SecurityRequestContext.Builder requestPath(String path) {
            this.requestPath = path;
            return this;
        }

        @Override
        public SecurityRequestContext.Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        @Override
        public SecurityRequestContext.Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        @Override
        public SecurityRequestContext.Builder clientAddress(String clientAddress) {
            this.clientAddress = clientAddress;
            return this;
        }

        @Override
        public SecurityRequestContext.Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        @Override
        public SecurityRequestContext build() {
            return new DefaultSecurityRequestContext(
                    requestId,
                    correlationId,
                    Instant.now(),
                    SecurityLifecyclePhase.RECEIVED,
                    Optional.empty(),
                    Set.of(),
                    requestMethod,
                    requestPath,
                    headers,
                    Optional.ofNullable(clientAddress),
                    attributes,
                    Optional.empty());
        }
    }
}
