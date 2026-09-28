package com.platform.http.api;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DispatchResultTest {

    @Test
    void mapTransformsSuccessValue() {
        DispatchResult<Integer> result = DispatchResult.success(2).map(v -> v * 10);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.orElseThrow()).isEqualTo(20);
    }

    @Test
    void mapOnFailurePassesThroughUnchanged() {
        RuntimeException error = new RuntimeException("boom");
        DispatchResult<Integer> result = DispatchResult.<Integer>failure(error).map(v -> v * 10);
        assertThat(result.isSuccess()).isFalse();
        assertThatThrownBy(result::orElseThrow).isSameAs(error);
    }

    @Test
    void flatMapChainsSuccessResults() {
        DispatchResult<String> result = DispatchResult.success(5).flatMap(v -> DispatchResult.success("value-" + v));
        assertThat(result.orElseThrow()).isEqualTo("value-5");
    }

    @Test
    void onSuccessAndOnFailureAreExclusivelyInvoked() {
        AtomicReference<Object> captured = new AtomicReference<>();
        DispatchResult.success("ok").onSuccess(captured::set).onFailure(e -> captured.set("should-not-run"));
        assertThat(captured.get()).isEqualTo("ok");

        captured.set(null);
        RuntimeException error = new RuntimeException("failure");
        DispatchResult.<String>failure(error).onSuccess(v -> captured.set("should-not-run")).onFailure(captured::set);
        assertThat(captured.get()).isSameAs(error);
    }

    @Test
    void orElseReturnsFallbackOnlyOnFailure() {
        assertThat(DispatchResult.success("value").orElse("fallback")).isEqualTo("value");
        assertThat(DispatchResult.<String>failure(new RuntimeException()).orElse("fallback")).isEqualTo("fallback");
    }

    @Test
    void consumeInvokesExactlyOneBranch() {
        AtomicReference<String> branch = new AtomicReference<>();
        DispatchResult.success("x").consume(v -> branch.set("success"), e -> branch.set("failure"));
        assertThat(branch.get()).isEqualTo("success");

        DispatchResult.<String>failure(new RuntimeException()).consume(v -> branch.set("success"), e -> branch.set("failure"));
        assertThat(branch.get()).isEqualTo("failure");
    }
}
