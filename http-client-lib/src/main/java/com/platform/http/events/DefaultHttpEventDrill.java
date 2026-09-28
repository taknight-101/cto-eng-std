package com.platform.http.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** Thread-safe {@link HttpEventDrill}; a throwing consumer is logged and skipped, never re-published. */
public final class DefaultHttpEventDrill implements HttpEventDrill {

    private static final Logger log = LoggerFactory.getLogger(DefaultHttpEventDrill.class);

    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    @Override
    public void publish(HttpEvent event) {
        for (Subscription subscription : subscriptions) {
            if (!subscription.types.contains(event.type())) {
                continue;
            }
            try {
                subscription.consumer.onEvent(event);
            } catch (Exception e) {
                log.warn("HTTP event consumer {} threw while handling {}; isolating failure",
                        subscription.consumer.getClass().getName(), event.type(), e);
            }
        }
    }

    @Override
    public HttpEventSubscription subscribe(HttpEventConsumer consumer) {
        return subscribe(EnumSet.allOf(HttpEventType.class), consumer);
    }

    @Override
    public HttpEventSubscription subscribe(Set<HttpEventType> types, HttpEventConsumer consumer) {
        Subscription subscription = new Subscription(Set.copyOf(types), consumer);
        subscriptions.add(subscription);
        return () -> subscriptions.remove(subscription);
    }

    private record Subscription(Set<HttpEventType> types, HttpEventConsumer consumer) {
    }
}
