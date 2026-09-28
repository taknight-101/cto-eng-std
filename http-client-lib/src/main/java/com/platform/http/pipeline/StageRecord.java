package com.platform.http.pipeline;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** One recorded step of a {@link RequestExecutionTrace}. */
public record StageRecord(
        String name, Instant startedAt, Instant completedAt, boolean success, String parentStage,
        Map<String, Object> metadata) {

    public StageRecord {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public Duration duration() {
        return Duration.between(startedAt, completedAt);
    }
}
