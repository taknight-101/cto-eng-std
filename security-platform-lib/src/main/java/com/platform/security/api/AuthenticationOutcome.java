package com.platform.security.api;

import org.springframework.security.core.Authentication;

import java.util.Objects;

/**
 * Result of a single {@link TokenAuthenticator} attempt.
 *
 * <p>{@link NotApplicable} lets multiple authenticators be tried in sequence (e.g. JWT then
 * OAuth2) without an authenticator that simply saw no relevant credential having to signal
 * "failure".
 */
public sealed interface AuthenticationOutcome {

    static AuthenticationOutcome success(Authentication authentication) {
        return new Success(authentication);
    }

    static AuthenticationOutcome failure(SecurityErrorCode errorCode, String reason) {
        return new Failure(errorCode, reason, null);
    }

    static AuthenticationOutcome failure(SecurityErrorCode errorCode, String reason, Throwable cause) {
        return new Failure(errorCode, reason, cause);
    }

    static AuthenticationOutcome notApplicable() {
        return NotApplicable.INSTANCE;
    }

    record Success(Authentication authentication) implements AuthenticationOutcome {
        public Success {
            Objects.requireNonNull(authentication, "authentication must not be null");
        }
    }

    record Failure(SecurityErrorCode errorCode, String reason, Throwable cause) implements AuthenticationOutcome {
    }

    final class NotApplicable implements AuthenticationOutcome {
        static final NotApplicable INSTANCE = new NotApplicable();

        private NotApplicable() {
        }
    }
}
