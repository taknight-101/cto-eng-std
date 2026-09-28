package com.platform.security.events;

import com.platform.security.api.SecurityEvent;
import com.platform.security.api.SecurityEventSubscription;
import com.platform.security.api.SecurityEventType;
import com.platform.security.api.SecurityRequestContext;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSecurityEventDrillTest {

    private final DefaultSecurityEventDrill drill = new DefaultSecurityEventDrill();
    private final SecurityRequestContext context = SecurityRequestContext.builder().build();

    @Test
    void allSubscribersReceiveMatchingEvents() {
        AtomicInteger count = new AtomicInteger();
        drill.subscribe(event -> count.incrementAndGet());
        drill.subscribe(event -> count.incrementAndGet());

        drill.publish(SecurityEvent.of(SecurityEventType.AUTHENTICATION_SUCCEEDED, context, "auth", Map.of()));

        assertThat(count.get()).isEqualTo(2);
    }

    @Test
    void typeFilteredSubscriptionOnlyReceivesMatchingTypes() {
        AtomicInteger matched = new AtomicInteger();
        drill.subscribe(Set.of(SecurityEventType.AUTHENTICATION_FAILED), event -> matched.incrementAndGet());

        drill.publish(SecurityEvent.of(SecurityEventType.AUTHENTICATION_SUCCEEDED, context, "auth", Map.of()));
        assertThat(matched.get()).isZero();

        drill.publish(SecurityEvent.of(SecurityEventType.AUTHENTICATION_FAILED, context, "auth", Map.of()));
        assertThat(matched.get()).isEqualTo(1);
    }

    @Test
    void throwingConsumerDoesNotPreventOtherConsumersOrPropagate() {
        AtomicInteger goodConsumerCalls = new AtomicInteger();
        drill.subscribe(event -> {
            throw new RuntimeException("misbehaving consumer");
        });
        drill.subscribe(event -> goodConsumerCalls.incrementAndGet());

        drill.publish(SecurityEvent.of(SecurityEventType.PROCESSING_COMPLETED, context, "completion", Map.of()));

        assertThat(goodConsumerCalls.get()).isEqualTo(1);
    }

    @Test
    void unsubscribeStopsFurtherDelivery() {
        AtomicInteger count = new AtomicInteger();
        SecurityEventSubscription subscription = drill.subscribe(event -> count.incrementAndGet());

        drill.publish(SecurityEvent.of(SecurityEventType.PROCESSING_COMPLETED, context, "completion", Map.of()));
        subscription.unsubscribe();
        drill.publish(SecurityEvent.of(SecurityEventType.PROCESSING_COMPLETED, context, "completion", Map.of()));

        assertThat(count.get()).isEqualTo(1);
    }
}
