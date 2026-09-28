package com.platform.http.pipeline;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Builds a {@link RequestExecutionTrace} for a single request execution. One instance per
 * request (not shared/thread-safe by design - it is created and discarded within the scope of
 * a single {@code HttpClient.execute} call).
 *
 * <pre>{@code
 * pipeline.stage("dispatch", () -> engine.execute(request));
 * }</pre>
 */
public final class HttpPipeline {

    private final String correlationId;
    private final List<StageRecord> stages = new ArrayList<>();

    private HttpPipeline(String correlationId) {
        this.correlationId = correlationId;
    }

    public static HttpPipeline start(String correlationId) {
        return new HttpPipeline(correlationId);
    }

    public <T> T stage(String name, Supplier<T> action) {
        Instant startedAt = Instant.now();
        try {
            T result = action.get();
            stages.add(new StageRecord(name, startedAt, Instant.now(), true, null, Map.of()));
            return result;
        } catch (RuntimeException e) {
            stages.add(new StageRecord(
                    name, startedAt, Instant.now(), false, null, Map.of("error", e.getClass().getSimpleName())));
            throw e;
        }
    }

    public RequestExecutionTrace trace() {
        return new RequestExecutionTrace(correlationId, List.copyOf(stages));
    }
}
