package com.platform.http.spi;

import java.time.Duration;
import java.util.Set;

/**
 * Explicit, configurable retry behavior. Defaults to retrying only idempotent requests (see
 * {@code HttpRequestSpec#idempotent()}) so a non-idempotent operation (e.g. POST) is never
 * silently retried and duplicated unless the caller explicitly marks it idempotent.
 */
public record RetryPolicy(
        int maxAttempts,
        Duration initialBackoff,
        double backoffMultiplier,
        Duration maxBackoff,
        Set<Integer> retryableStatusCodes,
        boolean idempotentOnly) {

    public static RetryPolicy none() {
        return new RetryPolicy(1, Duration.ZERO, 1.0, Duration.ZERO, Set.of(), true);
    }

    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, Duration.ofMillis(200), 2.0, Duration.ofSeconds(5), Set.of(502, 503, 504), true);
    }

    public Duration backoffFor(int attemptNumber) {
        long millis = (long) (initialBackoff.toMillis() * Math.pow(backoffMultiplier, Math.max(0, attemptNumber - 1)));
        return Duration.ofMillis(Math.min(millis, maxBackoff.toMillis()));
    }

    public boolean isRetryable(int status) {
        return retryableStatusCodes.contains(status);
    }
}
