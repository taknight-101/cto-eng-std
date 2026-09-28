package com.platform.http.pipeline;

import java.util.List;

/**
 * Declarative, inspectable record of the named stages a request passed through
 * (middleware, dispatch, downstream call, consumption), with per-stage timing and
 * success/failure. Deliberately not a distributed tracing implementation - integrate with
 * Micrometer/OpenTelemetry/logging by reading this trace, not by extending it.
 */
public record RequestExecutionTrace(String correlationId, List<StageRecord> stages) {

    public RequestExecutionTrace {
        stages = stages == null ? List.of() : List.copyOf(stages);
    }

    public static RequestExecutionTrace empty(String correlationId) {
        return new RequestExecutionTrace(correlationId, List.of());
    }
}
