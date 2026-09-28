package com.platform.example.events;

import com.platform.http.events.HttpEvent;
import com.platform.http.events.HttpEventConsumer;
import com.platform.http.events.HttpEventDrill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Application-specific HTTP failure event consumer, e.g. for alerting on downstream outages. */
@Component
public class DownstreamHttpEventConsumer implements HttpEventConsumer {

    private static final Logger log = LoggerFactory.getLogger("com.platform.example.http-audit");

    public DownstreamHttpEventConsumer(HttpEventDrill eventDrill) {
        eventDrill.subscribe(this);
    }

    @Override
    public void onEvent(HttpEvent event) {
        log.info("[HTTP-AUDIT] type={} correlationId={} uri={}", event.type(), event.correlationId(), event.request().uri());
    }
}
