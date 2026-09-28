package com.platform.http.events;

/** Consumer of {@link HttpEvent}s (logging, metrics, alerting, application handlers). */
@FunctionalInterface
public interface HttpEventConsumer {

    void onEvent(HttpEvent event);
}
