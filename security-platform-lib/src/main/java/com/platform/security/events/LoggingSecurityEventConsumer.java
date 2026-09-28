package com.platform.security.events;

import com.platform.security.api.SecurityEvent;
import com.platform.security.api.SecurityEventConsumer;
import com.platform.security.internal.Redaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default, always-safe-to-enable event consumer that logs via SLF4J with metadata redaction applied. */
public final class LoggingSecurityEventConsumer implements SecurityEventConsumer {

    private static final Logger log = LoggerFactory.getLogger("com.platform.security.events");

    @Override
    public void onEvent(SecurityEvent event) {
        var safeMetadata = Redaction.sanitizeMetadata(event.metadata());
        switch (event.type()) {
            case AUTHENTICATION_FAILED, INVALID_TOKEN, TOKEN_EXPIRED, MALFORMED_TOKEN, MISSING_CREDENTIALS,
                    AUTHORIZATION_FAILED, ACCESS_DENIED, MIDDLEWARE_FAILURE, SECURITY_CONTEXT_FAILURE,
                    UNEXPECTED_ERROR, REQUEST_REJECTED ->
                    log.warn("[security] {} correlationId={} stage={} metadata={}",
                            event.type(), event.correlationId(), event.stage(), safeMetadata, event.cause());
            default ->
                    log.info("[security] {} correlationId={} stage={} metadata={}",
                            event.type(), event.correlationId(), event.stage(), safeMetadata);
        }
    }
}
