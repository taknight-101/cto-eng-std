package com.platform.http.forwarding;

import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.HttpResponse;

import java.time.Duration;

/**
 * Explicit, observable decision about whether/how a request should be forwarded downstream -
 * deliberately a richer domain model than a boolean {@code shouldForward()}.
 */
public sealed interface ForwardingDecision {

    static ForwardingDecision forward(HttpRequestSpec downstreamRequest) {
        return new Forward(downstreamRequest);
    }

    static ForwardingDecision shortCircuit(HttpResponse<?> response) {
        return new ShortCircuit(response);
    }

    static ForwardingDecision reject(String reason) {
        return new Reject(reason);
    }

    static ForwardingDecision retry(Duration delay, HttpRequestSpec request) {
        return new Retry(delay, request);
    }

    static ForwardingDecision transform(HttpRequestSpec transformedRequest) {
        return new Transform(transformedRequest);
    }

    /** Forward the (possibly unmodified) request downstream. */
    record Forward(HttpRequestSpec downstreamRequest) implements ForwardingDecision {
    }

    /** Do not forward; respond immediately with the given response (e.g. a cache hit). */
    record ShortCircuit(HttpResponse<?> response) implements ForwardingDecision {
    }

    /** Do not forward; the request is not allowed to proceed (e.g. policy denial). */
    record Reject(String reason) implements ForwardingDecision {
    }

    /** Retry after a delay, e.g. because the downstream service is temporarily unavailable. */
    record Retry(Duration delay, HttpRequestSpec request) implements ForwardingDecision {
    }

    /** Forward, but with a transformed request (e.g. headers rewritten, path rewritten). */
    record Transform(HttpRequestSpec transformedRequest) implements ForwardingDecision {
    }
}
