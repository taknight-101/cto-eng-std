package com.platform.security.api;

/**
 * Deliberate lifecycle model for a single request's journey through the security pipeline.
 *
 * <p>The happy path is {@code RECEIVED -> AUTHENTICATING -> AUTHENTICATED -> AUTHORIZING ->
 * AUTHORIZED -> COMPLETED}. Each stage can instead terminate in a failure phase; unexpected
 * (non-security-domain) errors land in {@link #FAILED} rather than the expected
 * {@code *_FAILED} phases, so consumers can distinguish "the user was rejected" from
 * "the pipeline broke".
 */
public enum SecurityLifecyclePhase {

    RECEIVED,
    AUTHENTICATING,
    AUTHENTICATED,
    AUTHENTICATION_FAILED,
    AUTHORIZING,
    AUTHORIZED,
    AUTHORIZATION_FAILED,
    COMPLETED,
    FAILED;

    /** Whether this phase is a terminal state for the request's security processing. */
    public boolean isTerminal() {
        return switch (this) {
            case AUTHENTICATION_FAILED, AUTHORIZATION_FAILED, COMPLETED, FAILED -> true;
            default -> false;
        };
    }

    /** Whether this phase represents an expected or unexpected security failure. */
    public boolean isFailure() {
        return this == AUTHENTICATION_FAILED || this == AUTHORIZATION_FAILED || this == FAILED;
    }
}
