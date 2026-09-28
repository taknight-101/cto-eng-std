package com.platform.http.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/** Typed configuration for {@code http-client-lib}, bound from the {@code http-client} prefix. */
@ConfigurationProperties(prefix = "http-client")
public class HttpClientProperties {

    private boolean enabled = true;
    private Duration defaultTimeout = Duration.ofSeconds(10);
    private Duration connectTimeout = Duration.ofSeconds(5);

    private final Retry retry = new Retry();
    private final Events events = new Events();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(Duration defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Retry getRetry() {
        return retry;
    }

    public Events getEvents() {
        return events;
    }

    public static class Retry {
        private int maxAttempts = 3;
        private Duration initialBackoff = Duration.ofMillis(200);
        private double backoffMultiplier = 2.0;
        private Duration maxBackoff = Duration.ofSeconds(5);
        private List<Integer> retryableStatusCodes = List.of(502, 503, 504);
        private boolean idempotentOnly = true;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getInitialBackoff() {
            return initialBackoff;
        }

        public void setInitialBackoff(Duration initialBackoff) {
            this.initialBackoff = initialBackoff;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }

        public Duration getMaxBackoff() {
            return maxBackoff;
        }

        public void setMaxBackoff(Duration maxBackoff) {
            this.maxBackoff = maxBackoff;
        }

        public List<Integer> getRetryableStatusCodes() {
            return retryableStatusCodes;
        }

        public void setRetryableStatusCodes(List<Integer> retryableStatusCodes) {
            this.retryableStatusCodes = retryableStatusCodes;
        }

        public boolean isIdempotentOnly() {
            return idempotentOnly;
        }

        public void setIdempotentOnly(boolean idempotentOnly) {
            this.idempotentOnly = idempotentOnly;
        }
    }

    public static class Events {
        private boolean loggingConsumerEnabled = true;

        public boolean isLoggingConsumerEnabled() {
            return loggingConsumerEnabled;
        }

        public void setLoggingConsumerEnabled(boolean loggingConsumerEnabled) {
            this.loggingConsumerEnabled = loggingConsumerEnabled;
        }
    }
}
