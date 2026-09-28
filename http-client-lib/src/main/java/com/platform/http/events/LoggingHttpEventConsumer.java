package com.platform.http.events;

import com.platform.http.internal.Redaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default, always-safe-to-enable event consumer that logs via SLF4J with header redaction applied. */
public final class LoggingHttpEventConsumer implements HttpEventConsumer {

    private static final Logger log = LoggerFactory.getLogger("com.platform.http.events");

    @Override
    public void onEvent(HttpEvent event) {
        var safeHeaders = Redaction.sanitizeHeaders(event.request().headers());
        switch (event.type()) {
            case REQUEST_CONSTRUCTION_FAILED, SERIALIZATION_FAILED, CONNECTION_FAILED, DNS_FAILURE, TIMEOUT,
                    TLS_FAILURE, REQUEST_REJECTED, DOWNSTREAM_CLIENT_ERROR, DOWNSTREAM_SERVER_ERROR,
                    DESERIALIZATION_FAILED, FORWARDING_FAILED, MIDDLEWARE_FAILURE, UNEXPECTED_ERROR ->
                    log.warn("[http] {} correlationId={} stage={} uri={} headers={}",
                            event.type(), event.correlationId(), event.stage(), event.request().uri(), safeHeaders,
                            event.cause());
            default ->
                    log.info("[http] {} correlationId={} stage={} uri={}",
                            event.type(), event.correlationId(), event.stage(), event.request().uri());
        }
    }
}
