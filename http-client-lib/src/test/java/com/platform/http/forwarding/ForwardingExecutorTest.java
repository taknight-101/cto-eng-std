package com.platform.http.forwarding;

import com.platform.http.api.ForwardingException;
import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.HttpResponse;
import com.platform.http.pipeline.RequestExecutionTrace;
import com.platform.http.response.DefaultHttpResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ForwardingExecutorTest {

    private final HttpRequestSpec request = HttpRequestSpec.get("/orders").build();

    @Test
    void forwardDelegatesToDownstreamCaller() {
        ForwardingExecutor executor = new ForwardingExecutor(
                (req, local) -> ForwardingDecision.forward(req), req -> okResponse(req));

        HttpResponse<?> response = executor.execute(request, null);
        assertThat(response.status()).isEqualTo(200);
    }

    @Test
    void shortCircuitNeverCallsDownstream() {
        HttpResponse<?> cached = okResponse(request);
        ForwardingExecutor executor = new ForwardingExecutor(
                (req, local) -> ForwardingDecision.shortCircuit(cached),
                req -> {
                    throw new AssertionError("downstream should not be called");
                });

        assertThat(executor.execute(request, null)).isSameAs(cached);
    }

    @Test
    void rejectThrowsForwardingException() {
        ForwardingExecutor executor = new ForwardingExecutor(
                (req, local) -> ForwardingDecision.reject("policy denied"), req -> okResponse(req));

        assertThatThrownBy(() -> executor.execute(request, null))
                .isInstanceOf(ForwardingException.class)
                .hasMessageContaining("policy denied");
    }

    @Test
    void retryEventuallyForwardsAfterAttempts() {
        AtomicInteger evaluations = new AtomicInteger();
        ForwardingExecutor executor = new ForwardingExecutor(
                (req, local) -> evaluations.incrementAndGet() < 3
                        ? ForwardingDecision.retry(Duration.ZERO, req)
                        : ForwardingDecision.forward(req),
                req -> okResponse(req));

        HttpResponse<?> response = executor.execute(request, null);
        assertThat(response.status()).isEqualTo(200);
        assertThat(evaluations.get()).isEqualTo(3);
    }

    @Test
    void retryIsBoundedAndEventuallyFails() {
        ForwardingExecutor executor = new ForwardingExecutor(
                (req, local) -> ForwardingDecision.retry(Duration.ZERO, req), req -> okResponse(req));

        assertThatThrownBy(() -> executor.execute(request, null)).isInstanceOf(ForwardingException.class);
    }

    private static HttpResponse<?> okResponse(HttpRequestSpec req) {
        Instant now = Instant.now();
        return new DefaultHttpResponse<>(200, Map.of(), "ok", new byte[0], req, now, now,
                RequestExecutionTrace.empty(req.correlationId()));
    }
}
