package com.platform.example.events;

import com.platform.security.api.SecurityEvent;
import com.platform.security.api.SecurityEventConsumer;
import com.platform.security.api.SecurityEventDrill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Application-specific security event consumer, demonstrating that consumers subscribe without
 * coupling to the internal pipeline. A real implementation might forward to an audit store/SIEM.
 */
@Component
public class AuditSecurityEventConsumer implements SecurityEventConsumer {

    private static final Logger log = LoggerFactory.getLogger("com.platform.example.security-audit");

    public AuditSecurityEventConsumer(SecurityEventDrill eventDrill) {
        eventDrill.subscribe(this);
    }

    @Override
    public void onEvent(SecurityEvent event) {
        log.info("[SECURITY-AUDIT] type={} correlationId={} stage={}", event.type(), event.correlationId(), event.stage());
    }
}
