package com.platform.security.api;

/** Consumer of {@link SecurityEvent}s (logging, metrics, audit, SIEM, application handlers). */
@FunctionalInterface
public interface SecurityEventConsumer {

    void onEvent(SecurityEvent event);
}
