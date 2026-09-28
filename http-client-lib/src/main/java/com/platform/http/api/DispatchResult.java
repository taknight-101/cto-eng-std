package com.platform.http.api;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Outcome of a declarative dispatch, composable via {@link #map}, {@link #flatMap}, {@link
 * #onSuccess}, {@link #onFailure} and terminal {@link #consume}.
 *
 * <p>Deliberately a small dedicated type rather than reusing Reactor's {@code Mono} for this
 * public, synchronous "dispatch and consume" API: {@code Mono} requires subscription semantics
 * that are easy to misuse (a {@code Mono} that is never subscribed to silently does nothing).
 * Reactor remains available for advanced users via {@code HttpClient.executeReactive}.
 */
public sealed interface DispatchResult<T> {

    boolean isSuccess();

    <U> DispatchResult<U> map(Function<T, U> mapper);

    <U> DispatchResult<U> flatMap(Function<T, DispatchResult<U>> mapper);

    DispatchResult<T> onSuccess(Consumer<T> consumer);

    DispatchResult<T> onFailure(Consumer<Throwable> consumer);

    T orElse(T fallback);

    T orElseThrow();

    void consume(Consumer<T> onSuccess, Consumer<Throwable> onFailure);

    static <T> DispatchResult<T> success(T value) {
        return new Success<>(value);
    }

    static <T> DispatchResult<T> failure(Throwable error) {
        return new Failure<>(error);
    }

    record Success<T>(T value) implements DispatchResult<T> {

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public <U> DispatchResult<U> map(Function<T, U> mapper) {
            try {
                return DispatchResult.success(mapper.apply(value));
            } catch (RuntimeException e) {
                return DispatchResult.failure(e);
            }
        }

        @Override
        public <U> DispatchResult<U> flatMap(Function<T, DispatchResult<U>> mapper) {
            try {
                return mapper.apply(value);
            } catch (RuntimeException e) {
                return DispatchResult.failure(e);
            }
        }

        @Override
        public DispatchResult<T> onSuccess(Consumer<T> consumer) {
            consumer.accept(value);
            return this;
        }

        @Override
        public DispatchResult<T> onFailure(Consumer<Throwable> consumer) {
            return this;
        }

        @Override
        public T orElse(T fallback) {
            return value;
        }

        @Override
        public T orElseThrow() {
            return value;
        }

        @Override
        public void consume(Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
            onSuccess.accept(value);
        }
    }

    record Failure<T>(Throwable error) implements DispatchResult<T> {

        @Override
        public boolean isSuccess() {
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <U> DispatchResult<U> map(Function<T, U> mapper) {
            return (DispatchResult<U>) this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <U> DispatchResult<U> flatMap(Function<T, DispatchResult<U>> mapper) {
            return (DispatchResult<U>) this;
        }

        @Override
        public DispatchResult<T> onSuccess(Consumer<T> consumer) {
            return this;
        }

        @Override
        public DispatchResult<T> onFailure(Consumer<Throwable> consumer) {
            consumer.accept(error);
            return this;
        }

        @Override
        public T orElse(T fallback) {
            return fallback;
        }

        @Override
        public T orElseThrow() {
            if (error instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(error);
        }

        @Override
        public void consume(Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
            onFailure.accept(error);
        }
    }
}
