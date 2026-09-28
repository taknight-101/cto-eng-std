package com.platform.downstream.events;

import com.platform.security.api.SecurityEvent;
import com.platform.security.api.SecurityEventConsumer;
import com.platform.security.api.SecurityEventDrill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Distinctly-prefixed consumer so downstream-api's own security events are easy to tell apart in shared logs. */
@Component
public class DownstreamSecurityEventConsumer implements SecurityEventConsumer {

    private static final Logger log = LoggerFactory.getLogger("com.platform.downstream.security-audit");

    public DownstreamSecurityEventConsumer(SecurityEventDrill eventDrill) {
        eventDrill.subscribe(this);
    }

    @Override
    public void onEvent(SecurityEvent event) {
        log.info("[DOWNSTREAM-SECURITY-AUDIT] type={} correlationId={} stage={}",
                event.type(), event.correlationId(), event.stage());
    }
}
