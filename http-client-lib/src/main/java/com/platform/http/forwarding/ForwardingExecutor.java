package com.platform.http.forwarding;

import com.platform.http.api.ForwardingException;
import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.HttpResponse;

import java.time.Duration;
import java.util.function.Function;

/**
 * Consumes {@link ForwardingDecision}s from a {@link ForwardingPolicy}, guarding {@code Retry}
 * with a bounded attempt count so a misconfigured policy can never loop forever.
 */
public final class ForwardingExecutor {

    private static final int MAX_RETRY_ATTEMPTS = 5;

    private final ForwardingPolicy policy;
    private final Function<HttpRequestSpec, HttpResponse<?>> downstreamCaller;

    public ForwardingExecutor(ForwardingPolicy policy, Function<HttpRequestSpec, HttpResponse<?>> downstreamCaller) {
        this.policy = policy;
        this.downstreamCaller = downstreamCaller;
    }

    public HttpResponse<?> execute(HttpRequestSpec request, HttpResponse<?> localResponse) {
        ForwardingDecision decision = policy.evaluate(request, localResponse);
        int attempts = 0;
        while (true) {
            switch (decision) {
                case ForwardingDecision.Forward forward -> {
                    return downstreamCaller.apply(forward.downstreamRequest());
                }
                case ForwardingDecision.Transform transform -> {
                    return downstreamCaller.apply(transform.transformedRequest());
                }
                case ForwardingDecision.ShortCircuit shortCircuit -> {
                    return shortCircuit.response();
                }
                case ForwardingDecision.Reject reject -> throw new ForwardingException(
                        "Forwarding rejected: " + reject.reason(), null);
                case ForwardingDecision.Retry retry -> {
                    attempts++;
                    if (attempts > MAX_RETRY_ATTEMPTS) {
                        throw new ForwardingException(
                                "Exceeded max forwarding retry attempts (" + MAX_RETRY_ATTEMPTS + ")", null);
                    }
                    sleep(retry.delay());
                    decision = policy.evaluate(retry.request(), localResponse);
                }
            }
        }
    }

    private static void sleep(Duration delay) {
        try {
            if (!delay.isZero() && !delay.isNegative()) {
                Thread.sleep(delay.toMillis());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ForwardingException("Interrupted while waiting to retry forwarding", e);
        }
    }
}
