package com.platform.http.events;

import java.util.Set;

/**
 * Central HTTP failure/lifecycle event routing pipeline, the HTTP-side equivalent of {@code
 * com.platform.security.api.SecurityEventDrill} (independently implemented - see root README).
 *
 * <p>A misbehaving consumer must never break the primary HTTP pipeline.
 */
public interface HttpEventDrill {

    void publish(HttpEvent event);

    HttpEventSubscription subscribe(HttpEventConsumer consumer);

    HttpEventSubscription subscribe(Set<HttpEventType> types, HttpEventConsumer consumer);
}
