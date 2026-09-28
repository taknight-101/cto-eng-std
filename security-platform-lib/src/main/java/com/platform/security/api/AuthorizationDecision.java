package com.platform.security.api;

/**
 * Result of evaluating an {@link AuthorizationPolicy}. A richer domain model than a boolean so
 * denial reasons are observable in events and diagnostics.
 */
public sealed interface AuthorizationDecision {

    boolean allowed();

    static AuthorizationDecision allow() {
        return new Allow();
    }

    static AuthorizationDecision deny(String reason) {
        return new Deny(reason);
    }

    record Allow() implements AuthorizationDecision {
        @Override
        public boolean allowed() {
            return true;
        }
    }

    record Deny(String reason) implements AuthorizationDecision {
        @Override
        public boolean allowed() {
            return false;
        }
    }
}
