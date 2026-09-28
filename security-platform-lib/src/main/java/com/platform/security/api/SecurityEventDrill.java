package com.platform.security.api;

import java.util.Set;

/**
 * Central event/failure routing pipeline ("event drill"): security lifecycle occurrences are
 * published here and fanned out to every subscribed consumer.
 *
 * <p><strong>Failure isolation:</strong> a consumer that throws must never break the primary
 * security pipeline and must never prevent other consumers from receiving the event.
 * Implementations must catch and log (not rethrow, not re-publish as a new event - that would
 * risk infinite recursion) any exception raised by a consumer.
 */
public interface SecurityEventDrill {

    void publish(SecurityEvent event);

    SecurityEventSubscription subscribe(SecurityEventConsumer consumer);

    SecurityEventSubscription subscribe(Set<SecurityEventType> types, SecurityEventConsumer consumer);
}
