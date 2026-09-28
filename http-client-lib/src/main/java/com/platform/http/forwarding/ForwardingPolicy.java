package com.platform.http.forwarding;

import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.HttpResponse;

/**
 * Pluggable rule deciding whether a request should be forwarded downstream, short-circuited
 * with a local response, rejected, retried, or transformed before forwarding.
 */
@FunctionalInterface
public interface ForwardingPolicy {

    ForwardingDecision evaluate(HttpRequestSpec request, HttpResponse<?> localResponse);
}
