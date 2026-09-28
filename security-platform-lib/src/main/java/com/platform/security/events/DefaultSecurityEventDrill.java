package com.platform.security.events;

import com.platform.security.api.SecurityEvent;
import com.platform.security.api.SecurityEventConsumer;
import com.platform.security.api.SecurityEventDrill;
import com.platform.security.api.SecurityEventSubscription;
import com.platform.security.api.SecurityEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe {@link SecurityEventDrill}. A consumer that throws is logged and skipped; it
 * never breaks {@link #publish}, never affects other consumers, and is never re-published as
 * a new event (which would risk infinite recursion for a mis-behaving consumer).
 */
public final class DefaultSecurityEventDrill implements SecurityEventDrill {

    private static final Logger log = LoggerFactory.getLogger(DefaultSecurityEventDrill.class);

    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    @Override
    public void publish(SecurityEvent event) {
        for (Subscription subscription : subscriptions) {
            if (!subscription.types.contains(event.type())) {
                continue;
            }
            try {
                subscription.consumer.onEvent(event);
            } catch (Exception e) {
                log.warn("Security event consumer {} threw while handling {}; isolating failure",
                        subscription.consumer.getClass().getName(), event.type(), e);
            }
        }
    }

    @Override
    public SecurityEventSubscription subscribe(SecurityEventConsumer consumer) {
        return subscribe(EnumSet.allOf(SecurityEventType.class), consumer);
    }

    @Override
    public SecurityEventSubscription subscribe(Set<SecurityEventType> types, SecurityEventConsumer consumer) {
        Subscription subscription = new Subscription(Set.copyOf(types), consumer);
        subscriptions.add(subscription);
        return () -> subscriptions.remove(subscription);
    }

    private record Subscription(Set<SecurityEventType> types, SecurityEventConsumer consumer) {
    }
}
